package com.example.javarelay.service.Impl;

import com.example.javarelay.dto.DriverDto;
import com.example.javarelay.dto.Result;
import com.example.javarelay.entity.Order;
import com.example.javarelay.exception.StatusChangeException;
import com.example.javarelay.infra.RedisUtil;
import com.example.javarelay.mapper.DriverMapper;
import com.example.javarelay.mapper.OrderMapper;
import com.example.javarelay.mapper.OrderStatusLogMapper;
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
        RedisUtil.tryunLockDriver(driverId);

        //通知司机
        informService.informPassenger();

        return Result.success("取消订单成功");
    }

    @Override
    public Result FinishOrder(DriverDto driverDto) {
        return null;
    }

    @Override
    public Result ReceiverOrder(DriverDto driverDto) {
        return null;
    }

    private Boolean driverCheck(DriverDto driverDto){
        if(driverDto == null || driverDto.getDriverId() == null){
            return false;
        }
        return true;
    }
}
