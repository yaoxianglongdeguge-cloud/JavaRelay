package com.example.javarelay.service.Impl;

import com.example.javarelay.constant.RedisPefix;
import com.example.javarelay.dto.DriverDto;
import com.example.javarelay.dto.PassengerDto;
import com.example.javarelay.dto.Result;
import com.example.javarelay.entity.Driver;
import com.example.javarelay.entity.Order;
import com.example.javarelay.exception.StatusChangeException;
import com.example.javarelay.infocollect.InfoCollector;
import com.example.javarelay.infra.GeoUtil;
import com.example.javarelay.infra.RedisUtil;
import com.example.javarelay.mapper.DriverMapper;
import com.example.javarelay.mapper.OrderMapper;
import com.example.javarelay.mapper.OrderStatusLogMapper;
import com.example.javarelay.service.InformService;
import com.example.javarelay.service.OrderService;
import com.example.javarelay.service.PassengerOrderService;
import com.example.javarelay.service.StrategysService;
import com.example.javarelay.status.DriverStatusEnum;
import com.example.javarelay.status.OrderOperatorEnum;
import com.example.javarelay.status.OrderStatusEnum;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.apache.ibatis.ognl.OgnlOps.doubleValue;

@Service
public class PassengerOrderServiceImpl implements PassengerOrderService {

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
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisUtil redisUtil;


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

    public Result ReDispatchOrder(PassengerDto passengerDto){

        if(passengerCheck(passengerDto).equals(false)){
            return Result.fail("信息不全");
        }

        Long passengerId = passengerDto.getPassengerId();

        Order order;
        try{
            order = orderMapper.passengerSelectUnFinshed(passengerId);
        }catch(Exception e){
            return Result.fail("系统出问题");
        }

        if(order == null){
            return Result.fail("不存在需要重派的订单");
        }

        String status = OrderStatusEnum.of(order.getStatus()).getDesc();
        if(!status.equals("待派单")){
            return Result.fail("不存在需要重派的订单");
        }


        Result result = DispatchOrder(passengerDto,order);
        if(result.getCode() != 200){
            return result;
        }

        return Result.success("派单成功");
    }

    @Override
    public Result CancelOrder(PassengerDto passengerDto) {

        if(passengerCheck(passengerDto).equals(false)){
            return Result.fail("信息不全");
        }

        Long passengerId = passengerDto.getPassengerId();

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
        if(fromstatus >= OrderStatusEnum.ARRIVED.getCode()){
            return Result.fail("司机已接单，不能取消");
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
        informService.informDriver();

        return Result.success("取消订单成功");
    }

    @Override
    public Result FinishOrder(PassengerDto passengerDto) {

        if(passengerCheck(passengerDto).equals(false)){
            return Result.fail("信息不全");
        }

        Long passengerId = passengerDto.getPassengerId();

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
        if(fromstatus < OrderStatusEnum.ARRIVED.getCode()){
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
        informService.informDriver();

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

    private Result AssignDriver(PassengerDto passengerDto,Long orderId){

        Long passengerId = passengerDto.getPassengerId();

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

                break;

            }
        }


        return Result.success(driverDto);

    }

    private Result DispatchOrder(PassengerDto passengerDto,Order order){

        Result result = AssignDriver(passengerDto,order.getId());

        if(result.getCode() != 200){
            return result;
        }

        DriverDto driverDto = (DriverDto) result.getData();


        //确定路线
        Result resultPath = strategysService.findPathByStrategy(order.getId());


        //预估金额
        Result resultPreMoney = strategysService.preComputeMoney(order.getId());


        //通知司机
        informService.informDriver();

        return Result.success("派单成功");
    }

}
