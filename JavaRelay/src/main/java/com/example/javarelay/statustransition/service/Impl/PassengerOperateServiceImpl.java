package com.example.javarelay.statustransition.service.Impl;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.inform.constants.InformTypeEnum;
import com.example.javarelay.statustransition.dto.DriverDto;
import com.example.javarelay.statustransition.dto.PassengerDto;
import com.example.javarelay.dto.Result;
import com.example.javarelay.statustransition.entity.RouteInfo;
import com.example.javarelay.data.entity.Order;
import com.example.javarelay.statustransition.exception.StatusChangeException;
import com.example.javarelay.statustransition.infocollect.InfoCollector;
import com.example.javarelay.infra.RedisUtil;
import com.example.javarelay.data.mapper.OrderDetailMapper;
import com.example.javarelay.data.mapper.OrderMapper;
import com.example.javarelay.inform.service.InformService;
import com.example.javarelay.statustransition.service.OrderService;
import com.example.javarelay.statustransition.service.PassengerOperateService;
import com.example.javarelay.statustransition.service.StrategysService;
import com.example.javarelay.statustransition.statusenum.DriverStatusEnum;
import com.example.javarelay.statustransition.statusenum.OrderOperatorEnum;
import com.example.javarelay.statustransition.statusenum.OrderStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

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

        //插入数据采集器
        Result result2 = strategysService.putInfoCollector(orderNew.getId(),passengerDto);
        if(result2.getCode() != 200){
            return result2;
        }

       Result result = DispatchOrder(passengerDto,orderNew);

        if(result.getCode() != 200){
            return result;
        }

        return Result.success("分配成功,司机正在赶来");
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
            RedisUtil.tryunLockDriver(driverId);
        }

        //通知司机
        informService.informDriver(orderId,passengerId,driverId,
                InformTypeEnum.STATUS_CONFIRM.getCode(),"已结束");

        //计算金额
        strategysService.ComputeMoney(orderId);

        Result result = strategysService.deleteInfoCollector(orderId);
        if(result.getCode() != 200){
            return Result.fail("系统出问题");
        }


        return Result.success("订单已结束");

    }

    public Result ReComputeMoney(Long passengerId){

        Order order;
        try{
            order = orderMapper.passengerSelectUnFinshed(passengerId);
        }catch (Exception e){
            return Result.fail("系统出问题");
        }
        if(order != null){
            return Result.fail("订单还未结束");
        }

        strategysService.ComputeMoney(order.getId());

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
        Result result1;
        DriverDto driverDto;

        while(true){

            Order order = orderMapper.selectByPassenger(passengerId);


            if(order != null){
                String status = OrderStatusEnum.of(order.getStatus()).getDesc();
                if(status.equals("已取消") || status.equals("已完成")){
                    return Result.fail("订单已结束");
                }
            }

            result1 = strategysService.findDriverByStrategy(orderId);

            if(result1.getCode() == 200){

                driverDto = (DriverDto)result1.getData();

                if(!RedisUtil.tryLockDriver(driverDto.getDriverId())){
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
                    String reason = e.getMessage();
                    if("DRIVER_CHANGE_FAIL".equals(reason)){
                        //如果修改数据库失败就解锁司机
                        RedisUtil.tryunLockDriver(driverDto.getDriverId());
                        continue;

                    }else if("ORDER_CHANGE_FAIL".equals(reason)){
                        //如果修改数据库失败就解锁司机
                        RedisUtil.tryunLockDriver(driverDto.getDriverId());

                        return Result.fail("订单已结束");
                    }

                }

                RedisUtil.tryLockDriver(driverDto.getDriverId());//再尝试锁一下，防止状态不一致

                break;

            }
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
            RedisUtil.tryunLockDriver(driverId);
        }

        //通知司机
        informService.informDriver(orderId,passengerId,driverId,
                InformTypeEnum.STATUS_CONFIRM.getCode(),"已取消");

        Result result = strategysService.deleteInfoCollector(orderId);
        if(result.getCode() != 200){
            return Result.fail("系统出问题");
        }

        return Result.success("取消订单成功");
    }

    private Result DispatchOrder(PassengerDto passengerDto,Order order){

        Result result = AssignDriver(passengerDto.getPassengerId(),order.getId());

        if(result.getCode() != 200){
            return result;
        }

        DriverDto driverDto = (DriverDto) result.getData();

        //确定路线
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

        return Result.success("派单成功");
    }

    public Result ReDispatchOrder(Long orderId,Long passengerId) {

        Order order;
        InfoCollector infoCollector;
        while (true) {

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

            infoCollector = strategysService.getInfoCollector(orderId);
            if (infoCollector == null) continue;
            break;

        }


        Result result = AssignDriver(passengerId, order.getId());

        if (result.getCode() != 200) {
            return result;
        }

        DriverDto driverDto = (DriverDto) result.getData();

        //确定路线
        Result resultPath = strategysService.findPathByStrategy(order.getId());
        RouteInfo routeInfo = (RouteInfo) resultPath.getData();
        routeInfo.setStartAndEnd(
                infoCollector.getStartLng(),
                infoCollector.getStartLat(),
                infoCollector.getEndLng(),
                infoCollector.getEndLat()
        );

        routeInfo.setStartAddress(infoCollector.getStartAddress());
        routeInfo.setEndAddress(infoCollector.getEndAddress());

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

        return Result.success("派单成功");


        }
}