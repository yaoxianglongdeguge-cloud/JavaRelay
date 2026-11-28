package com.example.javarelay.orderstatus.service.Impl;

import com.example.javarelay.orderstatus.constants.Constants;
import com.example.javarelay.userinform.constants.InformTypeEnum;
import com.example.javarelay.orderstatus.dto.DriverDto;
import com.example.javarelay.dto.Result;
import com.example.javarelay.orderstatus.entity.Order;
import com.example.javarelay.orderstatus.exception.StatusChangeException;
import com.example.javarelay.infra.redis.RedisUtil;
import com.example.javarelay.orderstatus.mapper.OrderMapper;
import com.example.javarelay.orderstatus.dispatch.mqtask.messagecells.ReDispatchMessage;
import com.example.javarelay.orderstatus.service.DriverOperateService;
import com.example.javarelay.userinform.service.InformService;
import com.example.javarelay.orderstatus.service.OrderService;
import com.example.javarelay.orderstatus.service.StrategysService;
import com.example.javarelay.orderstatus.constants.statusenum.OrderOperatorEnum;
import com.example.javarelay.orderstatus.constants.statusenum.OrderStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class DriverOperateServiceImpl implements DriverOperateService {

    @Autowired
    OrderMapper orderMapper;

    @Autowired
    OrderService orderService;

    @Autowired
    StrategysService strategysService;

    @Autowired
    InformService informService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public Result CancelOrder(Long driverId) {

        //查找该用户未结束订单
        Order order;
        try{
            order = orderMapper.driverSelectUnFinshed(driverId);
        }catch (Exception e){
            return Result.fail("系统问题");
        }

        if(order == null){
            return Result.success("没有未完成订单");
        }

        Long orderId = order.getId();
        Integer fromstatus = order.getStatus();
        Long passengerId = order.getPassengerId();

        if(fromstatus.compareTo(OrderStatusEnum.ARRIVED.getCode()) >= 0){
            return Result.fail("已接到乘客，不能取消");
        }

        //修改数据库状态
        try{
            if (!orderService.cancelOrder(orderId, passengerId, fromstatus, driverId,
                    OrderOperatorEnum.DRIVER.getCode(), "司机取消")) {
                return Result.success("订单已经被取消");
            }

        }catch(StatusChangeException e) {

            return Result.success("订单已经被取消");
        }

        //释放redis锁
        redisUtil.tryunLockDriver(driverId);

        //通知用户
        informService.informPassenger(orderId,driverId,passengerId,
                InformTypeEnum.STATUS_CONFIRM.getCode(),"已取消");

        return Result.success("取消订单成功");
    }

    @Override
    public Result ArriveOrder(Long driverId) {

        Order order;
        try {
            order = orderMapper.driverSelectUnFinshed(driverId);
        } catch (Exception e) {
            return Result.fail("系统问题");
        }

        if (order == null) {
            return Result.success("没有未完成订单");
        }

        Integer from = order.getStatus();
        if (!Objects.equals(from, OrderStatusEnum.RECEIVED.getCode())) {
            return Result.fail("订单不是已接单状态");
        }

        try {
            if (!orderService.arriveOrder(order.getId(), order.getPassengerId(), driverId)) {
                return Result.success("订单状态已变化");
            }
        } catch (StatusChangeException e) {
            return Result.success("订单状态已变化");
        }

        informService.informPassenger(order.getId(), driverId, order.getPassengerId(),
                InformTypeEnum.STATUS_CONFIRM.getCode(), "司机已到达");

        return Result.success("已到达");
    }

    @Override
    public Result FinishOrder(Long driverId) {


        //查找该司机未结束订单
        Order order;
        try{
            order = orderMapper.driverSelectUnFinshed(driverId);
        }catch (Exception e){
            return Result.fail("系统问题");
        }

        if(order == null){
            return Result.success("没有未完成订单");
        }

        Long orderId = order.getId();
        Integer fromstatus = order.getStatus();
        if(fromstatus.compareTo(OrderStatusEnum.ARRIVED.getCode()) < 0){
            return Result.fail("未到达");
        }
        Long passengerId = order.getPassengerId();

        //修改数据库状态
        try{
            if (!orderService.finishOrder(orderId, passengerId, driverId)) {
                return Result.success("订单已经结束");
            }

        }catch(StatusChangeException e) {

            return Result.success("订单已经结束");
        }

        //释放redis锁
        if(driverId != null){
            redisUtil.tryunLockDriver(driverId);
        }

        //通知用户
        informService.informPassenger(orderId,driverId,passengerId,
                InformTypeEnum.STATUS_CONFIRM.getCode(),"已结束");

        //计算金额
        strategysService.ComputeMoney(orderId);


        return Result.success("订单已结束");
    }

    @Override
    public Result ReceiveOrder(DriverDto driverDto) {

        if(driverCheck(driverDto).equals(false)){
            return Result.fail("信息不全");
        }

        Long driverId = driverDto.getDriverId();

        //查找该司机未结束订单
        Order order;
        try{
            order = orderMapper.driverSelectUnFinshed(driverId);
        }catch (Exception e){
            return Result.fail("系统问题");
        }

        if(order == null){
            return Result.success("没有未完成订单");
        }

        Long orderId = order.getId();
        Integer fromstatus = order.getStatus();
        if(!fromstatus.equals(OrderStatusEnum.DISPATCHED.getCode())){
            return Result.fail("订单已结束");
        }
        if(driverDto.getIsReceive() == null){
            return Result.fail("请明确是否接单");
        }

        if(driverDto.getIsReceive().equals(Constants.DRIVER_RECEIVE)){
            return receive(driverId,order);
        }else if(driverDto.getIsReceive().equals(Constants.DRIVER_DISRECEIVE)){
            return disreceive(driverId,order);
        }


        return Result.success("接单成功");
    }

    private Result receive(Long driverId,Order order){

        Long orderId = order.getId();
        Long passengerId = order.getPassengerId();

        try{
            if (!orderService.receiveOrder(orderId, passengerId, driverId)) {
                return Result.success("订单已经结束");
            }

        }catch(StatusChangeException e) {

            return Result.success("订单已经结束");
        }

        informService.informPassenger(orderId,driverId,passengerId,InformTypeEnum.STATUS_CONFIRM.getCode(),"已接单");

        return Result.success("接单成功");

    }

    private Result disreceive(Long driverId,Order order){

        Long orderId = order.getId();
        Long passengerId = order.getPassengerId();

        eventPublisher.publishEvent(new ReDispatchMessage(orderId, passengerId,
                OrderOperatorEnum.DRIVER.getCode()));

        informService.informPassenger(orderId,driverId,passengerId,InformTypeEnum.STATUS_CONFIRM.getCode(),"已拒单");

        return Result.success("已拒单");
    }

    private Boolean driverCheck(DriverDto driverDto){
        if(driverDto == null || driverDto.getDriverId() == null){
            return false;
        }
        return true;
    }
}
