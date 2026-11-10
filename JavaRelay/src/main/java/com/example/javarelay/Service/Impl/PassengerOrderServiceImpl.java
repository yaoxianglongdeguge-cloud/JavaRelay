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
import com.example.javarelay.mapper.DriverMapper;
import com.example.javarelay.mapper.OrderMapper;
import com.example.javarelay.mapper.OrderStatusLogMapper;
import com.example.javarelay.service.OrderService;
import com.example.javarelay.service.PassengerOrderService;
import com.example.javarelay.service.StrategysService;
import com.example.javarelay.status.DriverStatusEnum;
import com.example.javarelay.status.OrderOperatorEnum;
import com.example.javarelay.status.OrderStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    private StringRedisTemplate redisTemplate;


    @Override
    public Result PutOrder(PassengerDto passengerDto) {

        if(passengerCheck(passengerDto).equals(false)){
            return Result.fail("信息不全");
        }

        Long passengerId = passengerDto.getPassengerId();


        //检查该用户是否有未结束订单
        Order order = orderMapper.selectByPassenger(passengerId);

        if(order != null){
            String status = OrderStatusEnum.of(order.getStatus()).getDesc();
            if(!status.equals("已取消") && !status.equals("已完成")){
                return Result.fail("存在未结束订单");
            }
        }

        //没有未结束订单，生成订单并插入

        Result result1;
        try{
            result1 = orderService.createAndInsertOrder(passengerDto);

        }catch(Exception e){
            return Result.fail(e.getMessage());
        }

        Order order1 = (Order)result1.getData();

        Result result2 = strategysService.putInfoCollector(order1.getId(),passengerDto);
        if(result2.getCode() != 200){
            return result2;
        }

        //开始分配司机
        Result result = AssignDriver(passengerDto,order1.getId());

        if(result.getCode() != 200){
            return result;
        }

        DriverDto driverDto = (DriverDto) result.getData();



        //确定路线



        //预估金额

        //通知司机


        return Result.success("分配成功,司机正在赶来");
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


                try{
                    orderService.changeOrderStatus(orderId,passengerId,OrderStatusEnum.WAITING_DISPATCH.getCode(),
                            OrderStatusEnum.DISPATCHED.getCode(), OrderOperatorEnum.PASSENGER.getCode(),
                            driverDto.getId(), DriverStatusEnum.LEISURE.getCode(),DriverStatusEnum.RECEIVING.getCode());

                }catch(StatusChangeException e) {
                    String reason = e.getMessage();
                    if("DRIVER_CHANGE_FAIL".equals(reason)){
                        continue;
                    }else if("ORDER_CHANGE_FAIL".equals(reason)){
                        return Result.fail("订单已结束");
                    }
                }

                break;

            }
        }


        return Result.success(driverDto);

    }


    @Override
    public Result CancelOrder(PassengerDto passengerDto) {
        return null;
    }

    @Override
    public Result FinishOrder(PassengerDto passengerDto) {
        return null;
    }



    private Boolean passengerCheck(PassengerDto passengerDto){
        if(passengerDto == null || passengerDto.getPassengerId() == null){
            return false;
        }
        return true;
    }

}
