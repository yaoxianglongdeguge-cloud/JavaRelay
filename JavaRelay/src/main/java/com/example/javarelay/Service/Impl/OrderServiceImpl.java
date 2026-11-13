package com.example.javarelay.service.Impl;

import com.example.javarelay.constant.Constants;
import com.example.javarelay.dto.PassengerDto;
import com.example.javarelay.dto.Result;
import com.example.javarelay.entity.Driver;
import com.example.javarelay.entity.Order;
import com.example.javarelay.entity.OrderStatusLog;
import com.example.javarelay.exception.BusinessException;
import com.example.javarelay.exception.StatusChangeException;
import com.example.javarelay.mapper.DriverMapper;
import com.example.javarelay.mapper.OrderMapper;
import com.example.javarelay.mapper.OrderStatusLogMapper;
import com.example.javarelay.service.OrderService;
import com.example.javarelay.status.OrderOperatorEnum;
import com.example.javarelay.status.OrderStatusEnum;
import org.apache.ibatis.annotations.Param;
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
    DriverMapper driverMapper;

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
            orderMapper.insert(order1);

        }catch(DuplicateKeyException e) {
            //用唯一索引防止重复插入
            throw new StatusChangeException("ORDER_INSERT_FAIL");

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
            throw new RuntimeException("订单流转记录插入失败");
        }

        return Result.success(order1);
    }

    @Transactional
    @Override
    public Result changeOrderStatus(Long orderId,Long passengerId,
                                    Integer fromStatus,Integer toStatus,Integer operator,
                                    Long driverId,Integer fromStatus2,Integer toStatus2){

        int rows1 = orderMapper.updateStatusWithOptimisticLock(orderId,fromStatus,toStatus,driverId);
        if(rows1 == 0){
            throw new StatusChangeException("ORDER_CHANGE_FAIL");
        }else if(rows1 < 0){
            throw new RuntimeException("数据库异常");
        }

        if(toStatus.equals(OrderStatusEnum.CANCELED.getCode()) ||
                toStatus.equals(OrderStatusEnum.FINISHED.getCode())){

            int rows3 = orderMapper.updateActiveFlag(orderId, Constants.ACTIVE_ORDER,Constants.FINISHED_ORDER);
            if(rows3 == 0){
                throw new StatusChangeException("ORDER_CHANGE_FAIL");
            }else if(rows3 < 0){
                throw new RuntimeException("数据库异常");
            }

        }

        OrderStatusLog orderStatusLog = new OrderStatusLog(
                orderId,
                fromStatus,
                toStatus,
                operator,
                passengerId
        );

        int rows = orderStatusLogMapper.insert(orderStatusLog);
        if (rows == 0) {
            throw new RuntimeException("订单流转记录插入失败");
        }else if(rows < 0){
            throw new RuntimeException("数据库异常");
        }

        int rows2 = driverMapper.updateStatusWithOptimisticLock(driverId,fromStatus2,toStatus2);
        if(driverId != null && rows2 == 0){
            throw new StatusChangeException("DRIVER_CHANGE_FAIL");
        }else if(driverId != null && rows2 < 0){
            throw new RuntimeException("数据库异常");
        }


        return null;
    }
}
