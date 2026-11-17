package com.example.javarelay.service.Impl;

import com.example.javarelay.constant.Constants;
import com.example.javarelay.constant.InformTypeEnum;
import com.example.javarelay.dto.DriverDto;
import com.example.javarelay.dto.Result;
import com.example.javarelay.entity.Order;
import com.example.javarelay.exception.StatusChangeException;
import com.example.javarelay.infra.RedisUtil;
import com.example.javarelay.mapper.DriverMapper;
import com.example.javarelay.mapper.OrderMapper;
import com.example.javarelay.mapper.OrderStatusLogMapper;
import com.example.javarelay.messagetask.MessageCells.ComputeMoneyMessage;
import com.example.javarelay.messagetask.MessageDispatcher;
import com.example.javarelay.service.DriverOrderService;
import com.example.javarelay.service.InformService;
import com.example.javarelay.service.OrderService;
import com.example.javarelay.service.StrategysService;
import com.example.javarelay.status.DriverStatusEnum;
import com.example.javarelay.status.OrderOperatorEnum;
import com.example.javarelay.status.OrderStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DriverOrderServiceImpl implements DriverOrderService {

    @Autowired
    OrderMapper orderMapper;

    @Autowired
    DriverMapper driverMapper;

    @Autowired
    OrderStatusLogMapper orderStatusLogMapper;

    @Autowired
    OrderService orderService;

    @Autowired
    StrategysService strategysService;

    @Autowired
    InformService informService;

    @Autowired
    MessageDispatcher messageDispatcher;

    @Override
    public Result CancelOrder(DriverDto driverDto) {

        if(driverCheck(driverDto).equals(false)){
            return Result.fail("信息不全");
        }

        Long driverId = driverDto.getDriverId();

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
            orderService.changeOrderStatus(orderId,passengerId,
                    fromstatus,
                    OrderStatusEnum.CANCELED.getCode(),
                    OrderOperatorEnum.DRIVER.getCode(),

                    driverId,
                    DriverStatusEnum.RECEIVING.getCode(),
                    DriverStatusEnum.LEISURE.getCode());

        }catch(StatusChangeException e) {

            return Result.success("订单已经被取消");
        }

        //释放redis锁
        RedisUtil.tryunLockDriver(driverId);

        //通知用户
        informService.informPassenger(orderId,passengerId,
                InformTypeEnum.STATUS_CONFIRM.getCode(),"已取消");

        return Result.success("取消订单成功");
    }

    @Override
    public Result FinishOrder(DriverDto driverDto) {

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
        if(fromstatus.compareTo(OrderStatusEnum.ARRIVED.getCode()) < 0){
            return Result.fail("未到达");
        }
        Long passengerId = order.getPassengerId();

        //修改数据库状态
        try{
            orderService.changeOrderStatus(orderId,passengerId,
                    fromstatus,
                    OrderStatusEnum.FINISHED.getCode(),
                    OrderOperatorEnum.DRIVER.getCode(),

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

        //通知用户
        informService.informPassenger(orderId,passengerId,
                InformTypeEnum.STATUS_CONFIRM.getCode(),"已结束");

        //计算金额
        strategysService.ComputeMoney(orderId);

        Result result = strategysService.deleteInfoCollector(orderId);
        if(result.getCode() != 200){
            return Result.fail("系统出问题");
        }


        return Result.success("订单已结束");
    }

    @Override
    public Result ReceiverOrder(DriverDto driverDto) {

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

        if(driverDto.getIsReceive().equals(Constants.DRIVER_RECEIVE)){
            return receiver(driverDto,order);
        }else if(driverDto.getIsReceive().equals(Constants.DRIVER_DISRECEIVE)){
            return disreceiver(driverDto,order);
        }


        return Result.success("接单成功");
    }

    private Result receiver(DriverDto driverDto,Order order){

        Long orderId = order.getId();
        Long passengerId = order.getPassengerId();
        Long driverId = driverDto.getDriverId();

        try{
            orderService.changeOrderStatus(orderId,passengerId,
                    OrderStatusEnum.DISPATCHED.getCode(),
                    OrderStatusEnum.RECEIVED.getCode(),
                    OrderOperatorEnum.DRIVER.getCode(),

                    null,null,null);

        }catch(StatusChangeException e) {

            return Result.success("订单已经结束");
        }

        informService.informPassenger(orderId,passengerId,InformTypeEnum.STATUS_CONFIRM.getCode(),"已接单");

        return Result.success("接单成功");

    }

    private Result disreceiver(DriverDto driverDto,Order order){

        Long orderId = order.getId();
        Long passengerId = order.getPassengerId();
        Long driverId = driverDto.getDriverId();

        try{
            orderService.changeOrderStatus(orderId,passengerId,
                    OrderStatusEnum.DISPATCHED.getCode(),
                    OrderStatusEnum.WAITING_DISPATCH.getCode(),
                    OrderOperatorEnum.DRIVER.getCode(),

                    null,null,null);

        }catch(StatusChangeException e) {

            return Result.success("订单已经结束");
        }

        ComputeMoneyMessage computeMoneyMessage = new ComputeMoneyMessage(orderId);
        computeMoneyMessage.setTopic(Constants.MESSAGE_REDISPATCH_TOPIC);

        if(!messageDispatcher.putMessage(computeMoneyMessage)){
            return Result.fail("信息投递失败");
        }

        return Result.success("已拒单");

    }

    private Boolean driverCheck(DriverDto driverDto){
        if(driverDto == null || driverDto.getDriverId() == null){
            return false;
        }
        return true;
    }
}
