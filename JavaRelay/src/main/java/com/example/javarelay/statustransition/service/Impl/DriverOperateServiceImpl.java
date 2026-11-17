package com.example.javarelay.statustransition.service.Impl;

import com.example.javarelay.statustransition.config.MessageTaskConfig;
import com.example.javarelay.statustransition.constants.Constants;
import com.example.javarelay.inform.constants.InformTypeEnum;
import com.example.javarelay.statustransition.dto.DriverDto;
import com.example.javarelay.dto.Result;
import com.example.javarelay.data.entity.Order;
import com.example.javarelay.statustransition.exception.StatusChangeException;
import com.example.javarelay.infra.RedisUtil;
import com.example.javarelay.data.mapper.DriverMapper;
import com.example.javarelay.data.mapper.OrderMapper;
import com.example.javarelay.data.mapper.OrderStatusLogMapper;
import com.example.javarelay.statustransition.messagetask.messagecells.ReDispatchMessage;
import com.example.javarelay.infra.messagetask.MessageDispatcher;
import com.example.javarelay.statustransition.service.DriverOperateService;
import com.example.javarelay.inform.service.InformService;
import com.example.javarelay.statustransition.service.OrderService;
import com.example.javarelay.statustransition.service.StrategysService;
import com.example.javarelay.statustransition.statusenum.DriverStatusEnum;
import com.example.javarelay.statustransition.statusenum.OrderOperatorEnum;
import com.example.javarelay.statustransition.statusenum.OrderStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DriverOperateServiceImpl implements DriverOperateService {

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

    private Result disreceive(Long driverId,Order order){

        Long orderId = order.getId();
        Long passengerId = order.getPassengerId();

        try{
            orderService.changeOrderStatus(orderId,passengerId,
                    OrderStatusEnum.DISPATCHED.getCode(),
                    OrderStatusEnum.WAITING_DISPATCH.getCode(),
                    OrderOperatorEnum.DRIVER.getCode(),

                    null,null,null);

        }catch(StatusChangeException e) {

            return Result.success("订单已经结束");
        }

        ReDispatchMessage reDispatchMessage = new ReDispatchMessage(orderId,passengerId);
        reDispatchMessage.setTopic(MessageTaskConfig.MESSAGE_REDISPATCH_TOPIC);

        if(!messageDispatcher.putMessage(reDispatchMessage)){
            return Result.fail("信息投递失败");
        }

        informService.informPassenger(orderId,passengerId,InformTypeEnum.STATUS_CONFIRM.getCode(),"已拒单");

        return Result.success("已拒单");

    }

    private Boolean driverCheck(DriverDto driverDto){
        if(driverDto == null || driverDto.getDriverId() == null){
            return false;
        }
        return true;
    }
}
