package com.example.javarelay.orderstatus.service.Impl;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.data.entity.OrderDetail;
import com.example.javarelay.userinform.constants.InformTypeEnum;
import com.example.javarelay.infra.mq.MessageDispatcher;
import com.example.javarelay.orderstatus.constants.Constants;
import com.example.javarelay.orderstatus.dto.DriverDto;
import com.example.javarelay.orderstatus.dto.PassengerDto;
import com.example.javarelay.dto.Result;
import com.example.javarelay.orderstatus.pojo.RouteInfo;
import com.example.javarelay.data.entity.Order;
import com.example.javarelay.orderstatus.exception.StatusChangeException;
import com.example.javarelay.infra.redis.RedisUtil;
import com.example.javarelay.data.mapper.OrderDetailMapper;
import com.example.javarelay.data.mapper.OrderMapper;
import com.example.javarelay.userinform.service.InformService;
import com.example.javarelay.orderstatus.mqtask.messagecells.ReDispatchMessage;
import com.example.javarelay.orderstatus.service.OrderService;
import com.example.javarelay.orderstatus.service.PassengerOperateService;
import com.example.javarelay.orderstatus.service.StrategysService;
import com.example.javarelay.orderstatus.constants.statusenum.DriverStatusEnum;
import com.example.javarelay.orderstatus.constants.statusenum.OrderOperatorEnum;
import com.example.javarelay.orderstatus.constants.statusenum.OrderStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class PassengerOperateServiceImpl implements PassengerOperateService {

    @Autowired
    OrderMapper orderMapper;

    @Autowired
    OrderDetailMapper orderDetailMapper;

    @Autowired
    OrderService orderService;

    @Autowired
    StrategysService strategysService;

    @Autowired
    InformService informService;

    @Autowired
    ApplicationContext ctx;

    @Autowired
    RedisUtil redisUtil;


    @Override
    public Result PutOrder(PassengerDto passengerDto) {

        if(passengerCheck(passengerDto).equals(false)){
            return Result.fail("信息不全");
        }

        Long passengerId = passengerDto.getPassengerId();


        //检查该用户是否有未结束订单
        Order order;
        try{
            order = orderMapper.passengerSelectUnFinshed(passengerId);
        }catch(Exception e){
            return Result.fail("系统出问题");
        }

        if(order != null){
            return Result.fail("存在已派单并且未结束订单");
        }

        //没有未结束订单，生成订单并插入

        Result result1;
        try{
            result1 = orderService.createAndInsertOrder(passengerDto);

        }catch(StatusChangeException e){
            return Result.fail(e.getMessage());
        }

        Order orderNew;
        try{
            orderNew = (Order)result1.getData();
        }catch (Exception e){
            return Result.fail("系统出问题");
        }



       Result dispatched= DispatchOrder(passengerDto,orderNew);


        if (dispatched.getCode() == 200) {
            return Result.success("分配成功,司机正在赶来");
        }

        MessageDispatcher dispatcher = ctx.getBean(MessageDispatcher.class);   // 调用时才解析
        boolean queued = dispatcher.addTask(new ReDispatchMessage(orderNew.getId(), passengerId));

        if (!queued) {
            orderService.cancelAfterDispatchFail(orderNew.getId(), passengerId,
                    OrderStatusEnum.WAITING_DISPATCH.getCode(), null, "派单队列已满");
            return Result.fail("当前订单过多，请稍后重试");
        }
        return Result.success("已下单，正在为您寻找车辆", orderNew);
    }


    @Override
    public Result FinishOrder(Long passengerId) {

        //查找该用户未结束订单
        Order order;
        try{
            order = orderMapper.passengerSelectUnFinshed(passengerId);
        }catch (Exception e){
            return Result.fail("系统问题");
        }

        if(order == null){
            return Result.success("没有未完成订单");
        }

        Long orderId = order.getId();
        Integer fromstatus = order.getStatus();
        if(fromstatus.compareTo(OrderStatusEnum.ARRIVED.getCode()) < 0){
            return Result.fail("司机未到达");
        }
        Long driverId = order.getDriverId();

        //修改数据库状态
        try{
            orderService.changeOrderStatus(orderId,passengerId,
                    fromstatus,
                    OrderStatusEnum.FINISHED.getCode(),
                    OrderOperatorEnum.PASSENGER.getCode(),

                    driverId,
                    DriverStatusEnum.RECEIVING.getCode(),
                    DriverStatusEnum.LEISURE.getCode());

        }catch(StatusChangeException e) {

            return Result.success("订单已经结束");
        }

        //释放redis锁
        if(driverId != null){
            redisUtil.tryunLockDriver(driverId);
        }

        //通知司机
        informService.informDriver(orderId,passengerId,driverId,
                InformTypeEnum.STATUS_CONFIRM.getCode(),"已结束");

        //计算金额
        strategysService.ComputeMoney(orderId);

        return Result.success("订单已结束");

    }

    public Result ReComputeMoney(Long orderId){

        Order order;
        try{
            order = orderMapper.selectById(orderId);
        }catch (Exception e){
            return Result.fail("系统出问题");
        }
        if(order != null && order.getActiveFlag() != null && order.getActiveFlag().equals(Constants.ACTIVE_ORDER)){
            return Result.fail("订单还未结束");
        }

        strategysService.ComputeMoney(orderId);

        return Result.success("订单金额计算中");

    }

    private Boolean passengerCheck(PassengerDto passengerDto){
        if(passengerDto == null || passengerDto.getPassengerId() == null){
            return false;
        }
        return true;
    }

    private Result AssignDriver(Long passengerId,Long orderId){

        //确定是否在服务范围内
        Result result = strategysService.findPassengerByStrategy(orderId);
        if(result.getCode() != 200){
            return result;
        }

        //筛选司机
        DriverDto driverDto = null;

        Order order = orderMapper.selectById(orderId);

        if(order == null || order.getActiveFlag() == null){
            return Result.fail("订单已结束");
        }

        long deadline = System.currentTimeMillis() + Constants.DISPATCH_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {

            Result picked = strategysService.findDriverByStrategy(orderId);

            if(picked.getCode() == 200){

                driverDto = (DriverDto)picked.getData();

                if(!redisUtil.tryLockDriver(driverDto.getDriverId())){
                    continue;
                }

                try{
                    orderService.changeOrderStatus(orderId,passengerId,
                            OrderStatusEnum.WAITING_DISPATCH.getCode(),
                            OrderStatusEnum.DISPATCHED.getCode(),
                            OrderOperatorEnum.PASSENGER.getCode(),

                            driverDto.getDriverId(),
                            DriverStatusEnum.LEISURE.getCode(),
                            DriverStatusEnum.RECEIVING.getCode());

                }catch(StatusChangeException e) {
                    //如果修改数据库失败就解锁司机
                    redisUtil.tryunLockDriver(driverDto.getDriverId());
                    String reason = e.getMessage();
                    if("DRIVER_CHANGE_FAIL".equals(reason)){
                        continue;

                    }else if("ORDER_CHANGE_FAIL".equals(reason)){
                        return Result.fail("订单已结束");
                    }
                    continue;

                }catch (Exception e) {
                    redisUtil.tryunLockDriver(driverDto.getDriverId());
                    log.error("派单落库失败 driverId={}", driverDto.getDriverId(), e);
                    continue;
                }

                redisUtil.tryLockDriver(driverDto.getDriverId());//再尝试锁一下，防止状态不一致

                break;

            }else{
                break;
            }
        }

        if (driverDto == null) {
            return Result.fail("附近暂无可用司机，请稍后重试");
        }

        return Result.success(driverDto);

    }

    @Override
    public Result CancelOrder(Long passengerId) {


        //查找该用户未结束订单
        Order order;
        try{
            order = orderMapper.passengerSelectUnFinshed(passengerId);
        }catch (Exception e){
            return Result.fail("系统问题");
        }

        if(order == null){
            return Result.success("没有未完成订单");
        }

        Long orderId = order.getId();
        Integer fromstatus = order.getStatus();
        if(fromstatus.compareTo(OrderStatusEnum.ARRIVED.getCode()) >= 0){
            return Result.fail("司机已到达，不能取消");
        }
        Long driverId = order.getDriverId();
        //如果没有driver，那就是null，也能匹配

        //修改数据库状态
        try{
            orderService.changeOrderStatus(orderId,passengerId,
                    fromstatus,
                    OrderStatusEnum.CANCELED.getCode(),
                    OrderOperatorEnum.PASSENGER.getCode(),

                    driverId,
                    DriverStatusEnum.RECEIVING.getCode(),
                    DriverStatusEnum.LEISURE.getCode());

        }catch(StatusChangeException e) {

            return Result.success("订单已经被取消");
        }

        //释放redis锁
        if(driverId != null){
            redisUtil.tryunLockDriver(driverId);
        }

        //通知司机
        informService.informDriver(orderId,passengerId,driverId,
                InformTypeEnum.STATUS_CONFIRM.getCode(),"已取消");

        return Result.success("取消订单成功");
    }

    private Result DispatchOrder(PassengerDto passengerDto,Order order){


        Result result = AssignDriver(passengerDto.getPassengerId(),order.getId());

        if (result.getCode() != 200) {
            // 没派到：订单还是 0，没有司机要还
            return result;
        }

        DriverDto driverDto = (DriverDto) result.getData();
        if (driverDto == null || driverDto.getDriverId() == null) {
            orderService.cancelAfterDispatchFail(order.getId(), passengerDto.getPassengerId(),
                    OrderStatusEnum.WAITING_DISPATCH.getCode(), null, "派单结果异常");
            return Result.fail("派单失败，请重试");
        }

        //确定路线
        try{

            Result resultPath = strategysService.findPathByStrategy(order.getId());
            RouteInfo routeInfo = (RouteInfo) resultPath.getData();
            routeInfo.setStartAndEnd(
                    passengerDto.getStartLng(),
                    passengerDto.getStartLat(),
                    passengerDto.getEndLng(),
                    passengerDto.getEndLat()
            );

            routeInfo.setStartAddress(passengerDto.getStartAddress());
            routeInfo.setEndAddress(passengerDto.getEndAddress());

            //预估金额
            Result resultPreMoney = strategysService.preComputeMoney(order.getId());
            BigDecimal preMoney = (BigDecimal)resultPreMoney.getData();

            try{
                orderDetailMapper.initOrderDetail(
                        order.getId(),
                        driverDto.getDriverId(),
                        preMoney,
                        JSON.toJSONString(routeInfo)
                );
            }catch (Exception e){
                return Result.fail("系统出问题");
            }


            //通知司机
            informService.informDriver(order.getId(),passengerDto.getPassengerId(),driverDto.getDriverId(),
                    InformTypeEnum.STATUS_CONFIRM.getCode(),"已派单");

        }catch (Exception e) {
            log.error("派单后续步骤失败，开始补偿 orderId={}", order.getId(), e);

            orderService.cancelAfterDispatchFail(order.getId(),passengerDto.getPassengerId(),
                    OrderStatusEnum.DISPATCHED.getCode(), driverDto.getDriverId(), e.getMessage());

            redisUtil.tryunLockDriver(driverDto.getDriverId());   // DB 补偿成功后再放 Redis
            return Result.fail("派单失败，请重试");
        }
        return Result.success("派单成功");
    }

    public Result ReDispatchOrder(Long orderId,Long passengerId) {

        Order order = null;

        try {
            order = orderMapper.passengerSelectUnFinshed(passengerId);
        } catch (Exception e) {
            return Result.fail("系统出问题");
        }

        if (order == null) {
            return Result.fail("不存在需要重派的订单");
        }

        String status = OrderStatusEnum.of(order.getStatus()).getDesc();
        if (!status.equals("待派单")) {
            return Result.fail("不存在需要重派的订单");
        }


        Result result = AssignDriver(passengerId, order.getId());

        if (result.getCode() != 200) {

            long ageMs = System.currentTimeMillis() - order.getCreateTime()
                    .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

            //防止无限重投
            if (ageMs > Constants.REDISPATCH_DEADLINE) {
                orderService.cancelAfterDispatchFail(orderId, passengerId,
                        OrderStatusEnum.WAITING_DISPATCH.getCode(), null, "长时间无可用司机");
                return Result.fail("派单超时，已取消");
            }

            ReDispatchMessage retry = new ReDispatchMessage(orderId, passengerId);
            retry.setExecuteTime(System.currentTimeMillis() + 500);   // 500ms 后重试
            MessageDispatcher dispatcher = ctx.getBean(MessageDispatcher.class);   // 调用时才解析

            if (!dispatcher.addTask(retry)) {
                orderService.cancelAfterDispatchFail(orderId, passengerId,
                        OrderStatusEnum.WAITING_DISPATCH.getCode(), null, "派单队列已满");
            }

            return Result.fail("匹配失败");

        }

        DriverDto driverDto = (DriverDto) result.getData();
        if (driverDto == null || driverDto.getDriverId() == null) {
            orderService.cancelAfterDispatchFail(order.getId(), passengerId,
                    OrderStatusEnum.WAITING_DISPATCH.getCode(), null, "派单结果异常");
            return Result.fail("派单失败，请重试");
        }

        try{

            OrderDetail orderDetail = null;
            try{
               orderDetail = orderDetailMapper.selectByOrderId(orderId);
            }catch (Exception e){
                throw new RuntimeException("数据库错误");
            }

            if(orderDetail == null){
                throw new RuntimeException("数据库错误");
            }

            RouteInfo route = orderDetail.deserializeRouteInfo();

            //确定路线
            Result resultPath = strategysService.findPathByStrategy(order.getId());
            RouteInfo routeInfo = (RouteInfo) resultPath.getData();
            routeInfo.setStartAndEnd(
                    route.getStartLng(),
                    route.getStartLat(),
                    route.getEndLng(),
                    route.getEndLat()
            );

            routeInfo.setStartAddress(route.getStartAddress());
            routeInfo.setEndAddress(route.getEndAddress());

            //预估金额
            Result resultPreMoney = strategysService.preComputeMoney(order.getId());
            BigDecimal preMoney = (BigDecimal) resultPreMoney.getData();

            try {
                orderDetailMapper.initOrderDetail(
                        order.getId(),
                        driverDto.getDriverId(),
                        preMoney,
                        JSON.toJSONString(routeInfo)
                );
            } catch (Exception e) {
                return Result.fail("系统出问题");
            }


            //通知司机
            informService.informDriver(orderId,passengerId,driverDto.getDriverId(),
                    InformTypeEnum.STATUS_CONFIRM.getCode(),"已派单");

        }catch (Exception e) {
            log.error("派单后续步骤失败，开始补偿 orderId={}", order.getId(), e);

            orderService.cancelAfterDispatchFail(order.getId(),passengerId,
                    OrderStatusEnum.DISPATCHED.getCode(), driverDto.getDriverId(), e.getMessage());

            redisUtil.tryunLockDriver(driverDto.getDriverId());   // DB 补偿成功后再放 Redis
            return Result.fail("派单失败，请重试");
        }

        return Result.success("派单成功");


    }

}