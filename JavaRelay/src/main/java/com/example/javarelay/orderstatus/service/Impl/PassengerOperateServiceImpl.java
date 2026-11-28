package com.example.javarelay.orderstatus.service.Impl;

import com.example.javarelay.userinform.constants.InformTypeEnum;
import com.example.javarelay.orderstatus.constants.Constants;
import com.example.javarelay.orderstatus.dto.PassengerDto;
import com.example.javarelay.dto.Result;
import com.example.javarelay.orderstatus.entity.Order;
import com.example.javarelay.orderstatus.exception.StatusChangeException;
import com.example.javarelay.infra.redis.RedisUtil;
import com.example.javarelay.orderstatus.mapper.OrderDetailMapper;
import com.example.javarelay.orderstatus.mapper.OrderMapper;
import com.example.javarelay.userinform.service.InformService;
import com.example.javarelay.orderstatus.dispatch.mqtask.messagecells.DispatchMessage;
import com.example.javarelay.orderstatus.service.OrderService;
import com.example.javarelay.orderstatus.service.PassengerOperateService;
import com.example.javarelay.orderstatus.service.StrategysService;
import com.example.javarelay.orderstatus.constants.statusenum.OrderOperatorEnum;
import com.example.javarelay.orderstatus.constants.statusenum.OrderStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;


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

        eventPublisher.publishEvent(new DispatchMessage(orderNew.getId(), passengerId, DispatchMessage.Step.PASSENGER));

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

        //通知司机
        informService.informDriver(orderId,passengerId,driverId,
                InformTypeEnum.STATUS_CONFIRM.getCode(),"已结束");

        //计算金额
        strategysService.ComputeMoney(orderId);

        return Result.success("订单已结束");

    }


    private Boolean passengerCheck(PassengerDto passengerDto){
        if(passengerDto == null || passengerDto.getPassengerId() == null){
            return false;
        }
        return true;
    }

    @Override
    public Result ReComputeMoney(Long orderId) {

        Order order;

        try {
            order = orderMapper.selectById(orderId);
        } catch (Exception e) {
            return Result.fail("系统出问题");
        }

        if (order != null && order.getActiveFlag() != null
                && order.getActiveFlag().equals(Constants.ACTIVE_ORDER)) {
            return Result.fail("订单还未结束");
        }

        strategysService.ComputeMoney(orderId);

        return Result.success("订单金额计算中");
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
            if (!orderService.cancelOrder(orderId, passengerId, fromstatus, driverId,
                    OrderOperatorEnum.PASSENGER.getCode(), "乘客取消")) {
                return Result.success("订单已经被取消");
            }

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


}
