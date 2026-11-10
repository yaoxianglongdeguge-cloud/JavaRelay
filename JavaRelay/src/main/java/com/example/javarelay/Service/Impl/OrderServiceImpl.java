package com.example.javarelay.service.Impl;

import com.example.javarelay.dto.PassengerDto;
import com.example.javarelay.dto.Result;
import com.example.javarelay.entity.Order;
import com.example.javarelay.entity.OrderStatusLog;
import com.example.javarelay.mapper.OrderMapper;
import com.example.javarelay.mapper.OrderStatusLogMapper;
import com.example.javarelay.service.OrderService;
import com.example.javarelay.status.OrderOperatorEnum;
import com.example.javarelay.status.OrderStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    OrderMapper orderMapper;

    @Autowired
    OrderStatusLogMapper orderStatusLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result createAndInsertOrder(PassengerDto passengerDto){

        //生成订单并插入
        Long passengerId = passengerDto.getPassengerId();

        Order order1 = new Order(passengerId,
                OrderStatusEnum.WAITING_DISPATCH.getCode());

        try{
            int rows = orderMapper.insert(order1);
            if (rows <= 0) {
                throw new RuntimeException("订单插入失败");
            }
        }catch(DuplicateKeyException e) {
            //用唯一索引防止重复插入
            throw new RuntimeException("您有正在进行的订单，请勿重复下单");

        }
        //主键回填，得到订单id

        //生成订单状态流转日志并插入
        OrderStatusLog orderStatusLog = new OrderStatusLog(
                order1.getId(),
                OrderStatusEnum.WAITING_BIRTH.getCode(),
                OrderStatusEnum.WAITING_DISPATCH.getCode(),
                OrderOperatorEnum.PASSENGER.getCode(),
                passengerId
        );

        int rows = orderStatusLogMapper.insert(orderStatusLog);
        if (rows <= 0) {
            throw new RuntimeException("订单插入失败");
        }

        return Result.success(order1);
    }

    public Result changeOrderStatus(Long orderId,Integer fromStatus,Integer toStatus){

        return null;
    }
}
