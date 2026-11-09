package com.example.javarelay.service.Impl;

import com.example.javarelay.dto.PassengerDto;
import com.example.javarelay.dto.Result;
import com.example.javarelay.entity.Order;
import com.example.javarelay.mapper.OrderMapper;
import com.example.javarelay.mapper.OrderStatusLogMapper;
import com.example.javarelay.service.OrderService;
import com.example.javarelay.service.PassengerOrderService;
import com.example.javarelay.service.SelectStrategyService;
import com.example.javarelay.status.OrderStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
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

        try{
            orderService.createAndInsertOrder(passengerDto).equals(false);
        }catch(Exception e){
            return Result.fail(e.getMessage());
        }

        //开始分配司机

        return Result.success("分配成功,司机正在赶来");
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
