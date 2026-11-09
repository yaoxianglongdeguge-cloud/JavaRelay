package com.example.javarelay.Service.Impl;

import com.example.javarelay.Dto.PassengerDto;
import com.example.javarelay.Dto.Result;
import com.example.javarelay.Entity.Order;
import com.example.javarelay.Entity.OrderStatusLog;
import com.example.javarelay.Entity.Passenger;
import com.example.javarelay.Mapper.OrderMapper;
import com.example.javarelay.Mapper.OrderStatusLogMapper;
import com.example.javarelay.SelectStrategy.SelectStrategy;
import com.example.javarelay.Service.OrderService;
import com.example.javarelay.Service.PassengerOrderService;
import com.example.javarelay.Service.SelectStrategyService;
import com.example.javarelay.Status.OrderOperatorEnum;
import com.example.javarelay.Status.OrderStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Map;

public class PassengerOrderServiceImpl implements PassengerOrderService {

    @Autowired
    OrderMapper orderMapper;

    @Autowired
    OrderStatusLogMapper orderStatusLogMapper;

    @Autowired
    OrderService orderService;

    @Autowired
    SelectStrategyService selectStrategyService;

    
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
            if(!status.equals("已取消") && status.equals("已完成")){
                return Result.fail("存在未结束订单");
            }
        }

        //没有未结束订单，生成订单并插入

        orderService.createAndInsertOrder(passengerDto);

        return Result.success("已生成订单");
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
        if(passengerDto == null || passengerDto.getPassengerId() == null ||
                passengerDto.getStartLat() == null || passengerDto.getStartLng() == null||
                passengerDto.getEndLat() == null || passengerDto.getEndLng() == null){
            return false;
        }
        return true;
    }

}
