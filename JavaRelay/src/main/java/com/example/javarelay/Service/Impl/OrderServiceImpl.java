package com.example.javarelay.Service.Impl;

import com.example.javarelay.Dto.PassengerDto;
import com.example.javarelay.Entity.Order;
import com.example.javarelay.Entity.OrderStatusLog;
import com.example.javarelay.Mapper.OrderMapper;
import com.example.javarelay.Mapper.OrderStatusLogMapper;
import com.example.javarelay.Service.OrderService;
import com.example.javarelay.Status.OrderOperatorEnum;
import com.example.javarelay.Status.OrderStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

public class OrderServiceImpl implements OrderService {

    @Autowired
    OrderMapper orderMapper;

    @Autowired
    OrderStatusLogMapper orderStatusLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean createAndInsertOrder(PassengerDto passengerDto) {

        //生成订单并插入
        Long passengerId = passengerDto.getPassengerId();
        BigDecimal startLng = passengerDto.getStartLng();
        BigDecimal startLat = passengerDto.getStartLat();
        BigDecimal endLng = passengerDto.getEndLng();
        BigDecimal endLat = passengerDto.getEndLat();

        Order order1 = new Order(passengerId,startLng,startLat,endLat,endLng,
                OrderStatusEnum.WAITING_DISPATCH.getCode());

        orderMapper.insert(order1);
        //主键回填，得到订单id

        //生成订单状态流转日志并插入
        OrderStatusLog orderStatusLog = new OrderStatusLog(
                order1.getId(),
                OrderStatusEnum.WAITING_BIRTH.getCode(),
                OrderStatusEnum.WAITING_DISPATCH.getCode(),
                OrderOperatorEnum.PASSENGER.getCode(),
                passengerId
        );

        orderStatusLogMapper.insert(orderStatusLog);

        return true;
    }
}
