package com.example.javarelay.orderstatus.service.Impl;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.data.entity.OrderDetail;
import com.example.javarelay.userinform.constants.InformTypeEnum;
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
import com.example.javarelay.orderstatus.mqtask.messagecells.AssignDriverMessage;
import com.example.javarelay.orderstatus.mqtask.messagecells.AssignPassengerMessage;
import com.example.javarelay.orderstatus.mqtask.messagecells.DispatchMessage;
import com.example.javarelay.orderstatus.mqtask.messagecells.AssignPathMessage;
import com.example.javarelay.orderstatus.service.OrderService;
import com.example.javarelay.orderstatus.service.PassengerOperateService;
import com.example.javarelay.orderstatus.service.StrategysService;
import com.example.javarelay.orderstatus.constants.statusenum.DriverStatusEnum;
import com.example.javarelay.orderstatus.constants.statusenum.OrderOperatorEnum;
import com.example.javarelay.orderstatus.constants.statusenum.OrderStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
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

    @Autowired
    private ApplicationEventPublisher eventPublisher;


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

        eventPublisher.publishEvent(new DispatchMessage(orderNew.getId(), passengerId));

        return Result.success("已下单，正在为您寻找车辆", orderNew);
    }


    public Result ReDispatchOrder(Long orderId,Long passengerId) {

        Order order = null;

        try {
            order = orderMapper.passengerSelectUnFinshed(passengerId);
        } catch (Exception e) {
            return Result.fail("系统出问题");
        }

        if (order == null || !OrderStatusEnum.of(order.getStatus()).getDesc().equals("待派单")) {
            return Result.fail("不存在需要派的订单");
        }


        Result result = assignDriverInternal(passengerId, order.getId());

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
            retry.setExecuteTime(System.currentTimeMillis() + 500);   // 500ms 后重试// 调用时才解析

            eventPublisher.publishEvent(retry);

            return Result.fail("匹配失败");

        }

        DriverDto driverDto = (DriverDto) result.getData();
        if (driverDto == null || driverDto.getDriverId() == null) {
            orderService.cancelAfterDispatchFail(order.getId(), passengerId,
                    OrderStatusEnum.WAITING_DISPATCH.getCode(), null, "派单结果异常");
            return Result.fail("派单失败，请重试");
        }

        // 派单成功 → 投递路线与预估消息（路线/预估/通知统一由 AssignPath 处理）
        eventPublisher.publishEvent(new AssignPathMessage(orderId, passengerId));

        return Result.success("派单成功");


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

    public Result AssignPassenger(Long orderId,Long passengerId){

        Order order = null;

        try {
            order = orderMapper.passengerSelectUnFinshed(passengerId);
        } catch (Exception e) {
            orderService.cancelAfterDispatchFail(orderId, passengerId,
                    OrderStatusEnum.WAITING_DISPATCH.getCode(), null, "系统问题");
            return Result.fail("系统出问题");
        }

        if (order == null || !OrderStatusEnum.of(order.getStatus()).getDesc().equals("待派单")) {
            return Result.fail("不存在需要派的订单");
        }

        //确定用户是否满足打车条件
        Result result = strategysService.findPassengerByStrategy(orderId);
        if(result.getCode() != 200){
            orderService.cancelAfterDispatchFail(orderId, passengerId,
                    OrderStatusEnum.WAITING_DISPATCH.getCode(), null, "用户不符合打车条件");
            return result;
        }

        // 校验通过 → 投递派单消息
        eventPublisher.publishEvent(new AssignDriverMessage(orderId, passengerId));

        return Result.success("开始寻找司机");
    }

    @Override
    public Result AssignDriver(Long orderId, Long passengerId) {

        Result result = assignDriverInternal(passengerId, orderId);

        if (result == null || result.getCode() != 200) {
            return result;
        }

        // 派单成功 → 投递路线与预估消息
        eventPublisher.publishEvent(new AssignPathMessage(orderId, passengerId));

        return result;
    }

    @Override
    public Result AssignPath(Long orderId, Long passengerId) {

        Order order;
        try {
            order = orderMapper.selectById(orderId);
        } catch (Exception e) {
            return Result.fail("系统出问题");
        }

        if (order == null) {
            return Result.fail("订单不存在");
        }

        // 前置守卫：必须已派单。否则 driver_id 会以 null 写入并锁死，导致后续无法正确派单
        if (order.getStatus() == null
                || !order.getStatus().equals(OrderStatusEnum.DISPATCHED.getCode())) {
            return Result.success("订单未派单，跳过路线计算");
        }

        OrderDetail orderDetail;
        try {
            orderDetail = orderDetailMapper.selectByOrderId(orderId);
        } catch (Exception e) {
            return Result.fail("系统出问题");
        }

        if (orderDetail == null) {
            return Result.fail("订单详情不存在");
        }

        // 幂等：已经算过就直接返回
        if (orderDetail.getEstimatedPrice() != null) {
            return Result.success("路线与预估已就绪，跳过");
        }

        RouteInfo base = orderDetail.deserializeRouteInfo();

        if (base == null) {
            return Result.fail("路线快照缺失");
        }

        Result resultPath = strategysService.findPathByStrategy(orderId);

        if (resultPath == null || resultPath.getCode() != 200) {
            return Result.fail("路线计算失败");
        }

        RouteInfo routeInfo = (RouteInfo) resultPath.getData();
        routeInfo.setStartAndEnd(base.getStartLng(), base.getStartLat(), base.getEndLng(), base.getEndLat());
        routeInfo.setStartAddress(base.getStartAddress());
        routeInfo.setEndAddress(base.getEndAddress());

        Result resultPreMoney = strategysService.preComputeMoney(orderId);
        BigDecimal preMoney = resultPreMoney == null ? null : (BigDecimal) resultPreMoney.getData();

        if (preMoney == null) {
            return Result.fail("预估价计算失败");
        }

        int updated;
        try {
            updated = orderDetailMapper.initOrderDetail(
                    orderId, order.getDriverId(), preMoney, JSON.toJSONString(routeInfo));
        } catch (Exception e) {
            log.error("路线落库失败 orderId={}", orderId, e);
            return Result.fail("系统出问题");
        }

        if (updated == 0) {
            // 已被其他链路写入，属于幂等命中
            return Result.success("路线已由其他链路写入，跳过");
        }

        // 通知司机（新链路只有这里发一次）
        informService.informDriver(orderId, passengerId, order.getDriverId(),
                InformTypeEnum.STATUS_CONFIRM.getCode(), "已派单");

        return Result.success("路线与预估完成");
    }

    private Result assignDriverInternal(Long passengerId,Long orderId){

        Order order = null;

        try {
            order = orderMapper.passengerSelectUnFinshed(passengerId);
        } catch (Exception e) {
            orderService.cancelAfterDispatchFail(orderId, passengerId,
                    OrderStatusEnum.WAITING_DISPATCH.getCode(), null, "系统问题");
            return Result.fail("系统出问题");
        }

        if (order == null || !OrderStatusEnum.of(order.getStatus()).getDesc().equals("待派单")) {
            return Result.fail("不存在需要派的订单");
        }

        //筛选司机
        DriverDto driverDto = null;

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


    //    private Result DispatchOrder(PassengerDto passengerDto,Order order){
//
//
//        Result result = AssignDriver(passengerDto.getPassengerId(),order.getId());
//
//        if (result.getCode() != 200) {
//            // 没派到：订单还是 0，没有司机要还
//            return result;
//        }
//
//        DriverDto driverDto = (DriverDto) result.getData();
//        if (driverDto == null || driverDto.getDriverId() == null) {
//            orderService.cancelAfterDispatchFail(order.getId(), passengerDto.getPassengerId(),
//                    OrderStatusEnum.WAITING_DISPATCH.getCode(), null, "派单结果异常");
//            return Result.fail("派单失败，请重试");
//        }
//
//        //确定路线
//        try{
//
//            Result resultPath = strategysService.findPathByStrategy(order.getId());
//            RouteInfo routeInfo = (RouteInfo) resultPath.getData();
//            routeInfo.setStartAndEnd(
//                    passengerDto.getStartLng(),
//                    passengerDto.getStartLat(),
//                    passengerDto.getEndLng(),
//                    passengerDto.getEndLat()
//            );
//
//            routeInfo.setStartAddress(passengerDto.getStartAddress());
//            routeInfo.setEndAddress(passengerDto.getEndAddress());
//
//            //预估金额
//            Result resultPreMoney = strategysService.preComputeMoney(order.getId());
//            BigDecimal preMoney = (BigDecimal)resultPreMoney.getData();
//
//            try{
//                orderDetailMapper.initOrderDetail(
//                        order.getId(),
//                        driverDto.getDriverId(),
//                        preMoney,
//                        JSON.toJSONString(routeInfo)
//                );
//            }catch (Exception e){
//                return Result.fail("系统出问题");
//            }
//
//
//            //通知司机
//            informService.informDriver(order.getId(),passengerDto.getPassengerId(),driverDto.getDriverId(),
//                    InformTypeEnum.STATUS_CONFIRM.getCode(),"已派单");
//
//        }catch (Exception e) {
//            log.error("派单后续步骤失败，开始补偿 orderId={}", order.getId(), e);
//
//            orderService.cancelAfterDispatchFail(order.getId(),passengerDto.getPassengerId(),
//                    OrderStatusEnum.DISPATCHED.getCode(), driverDto.getDriverId(), e.getMessage());
//
//            redisUtil.tryunLockDriver(driverDto.getDriverId());   // DB 补偿成功后再放 Redis
//            return Result.fail("派单失败，请重试");
//        }
//        return Result.success("派单成功");
//    }
}
