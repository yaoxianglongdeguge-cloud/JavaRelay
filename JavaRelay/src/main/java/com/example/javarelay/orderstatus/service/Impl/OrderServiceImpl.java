package com.example.javarelay.orderstatus.service.Impl;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.data.entity.Order;
import com.example.javarelay.data.entity.OrderDetail;
import com.example.javarelay.data.entity.OrderStatusLog;
import com.example.javarelay.data.mapper.DriverMapper;
import com.example.javarelay.data.mapper.OrderDetailMapper;
import com.example.javarelay.data.mapper.OrderMapper;
import com.example.javarelay.data.mapper.OrderStatusLogMapper;
import com.example.javarelay.dto.Result;
import com.example.javarelay.orderstatus.config.StrategyTypeConfig;
import com.example.javarelay.orderstatus.constants.Constants;
import com.example.javarelay.orderstatus.constants.statusenum.OrderOperatorEnum;
import com.example.javarelay.orderstatus.constants.statusenum.OrderStatusEnum;
import com.example.javarelay.orderstatus.dto.PassengerDto;
import com.example.javarelay.orderstatus.exception.StatusChangeException;
import com.example.javarelay.orderstatus.pojo.RouteInfo;
import com.example.javarelay.orderstatus.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    OrderMapper orderMapper;

    @Autowired
    DriverMapper driverMapper;

    @Autowired
    OrderStatusLogMapper orderStatusLogMapper;

    @Autowired
    OrderDetailMapper orderDetailMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result createAndInsertOrder(PassengerDto passengerDto) {

        Long passengerId = passengerDto.getPassengerId();

        Order order = new Order(passengerId);
        order.setStatus(OrderStatusEnum.WAITING_DISPATCH.getCode());
        order.setActiveFlag(Constants.ACTIVE_ORDER);

        try {
            orderMapper.insertOrder(order);
        } catch (DuplicateKeyException e) {
            throw new StatusChangeException("ORDER_INSERT_FAIL");
        }

        writeLog(order.getId(), OrderStatusEnum.WAITING_BIRTH.getCode(),
                OrderStatusEnum.WAITING_DISPATCH.getCode(), OrderOperatorEnum.PASSENGER.getCode(), passengerId);

        OrderDetail detail = new OrderDetail();
        detail.setOrderId(order.getId());
        detail.setPassengerId(passengerId);
        detail.setAcpriceType(passengerDto.getComputeMoneyType());
        detail.setEspriceType(StrategyTypeConfig.PRECOMPUTE_MONEY_TYPE);
        detail.setDriverType(passengerDto.getSelectDriverStrategy());
        detail.setPassengerType(StrategyTypeConfig.PASSENGER_SELECT_TYPE);
        detail.setPathType(StrategyTypeConfig.PATH_SELECT_TYPE);
        detail.setDataStage(0);

        RouteInfo route = new RouteInfo();
        route.setStartAndEnd(passengerDto.getStartLng(), passengerDto.getStartLat(),
                passengerDto.getEndLng(), passengerDto.getEndLat());
        route.setStartAddress(passengerDto.getStartAddress());
        route.setEndAddress(passengerDto.getEndAddress());
        detail.serializeRouteInfo(route);

        try {
            orderDetailMapper.insertDetail(detail);
        } catch (DuplicateKeyException e) {
            throw new StatusChangeException("订单详情表插入失败");
        }

        return Result.success(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean dispatchOrder(Long orderId, Long passengerId, Long driverId) {

        if (orderMapper.dispatch(orderId, driverId) == 0) {
            return false;
        }

        if (driverMapper.occupy(driverId) == 0) {
            throw new StatusChangeException("DRIVER_CHANGE_FAIL");
        }

        writeLog(orderId, OrderStatusEnum.WAITING_DISPATCH.getCode(),
                OrderStatusEnum.DISPATCHED.getCode(), OrderOperatorEnum.PASSENGER.getCode(), passengerId);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveRouteAndPrice(Long orderId, Long driverId,
                                     BigDecimal estimatedPrice, String routeSnapshot) {

        return orderDetailMapper.saveRouteAndPrice(orderId, driverId, estimatedPrice, routeSnapshot) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean receiveOrder(Long orderId, Long passengerId, Long driverId) {

        if (orderMapper.receive(orderId, driverId) == 0) {
            return false;
        }

        writeLog(orderId, OrderStatusEnum.DISPATCHED.getCode(),
                OrderStatusEnum.RECEIVED.getCode(), OrderOperatorEnum.DRIVER.getCode(), passengerId);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean arriveOrder(Long orderId, Long passengerId, Long driverId) {

        if (orderMapper.arrive(orderId, driverId) == 0) {
            return false;
        }

        writeLog(orderId, OrderStatusEnum.RECEIVED.getCode(),
                OrderStatusEnum.ARRIVED.getCode(), OrderOperatorEnum.DRIVER.getCode(), passengerId);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean finishOrder(Long orderId, Long passengerId, Long driverId) {

        if (orderMapper.finish(orderId) == 0) {
            return false;
        }

        if (driverId != null && driverMapper.release(driverId) == 0) {
            throw new StatusChangeException("DRIVER_CHANGE_FAIL");
        }

        writeLog(orderId, OrderStatusEnum.ARRIVED.getCode(),
                OrderStatusEnum.FINISHED.getCode(), OrderOperatorEnum.DRIVER.getCode(), passengerId);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean settleOrder(Long orderId, BigDecimal actualPrice) {

        return orderDetailMapper.settle(orderId, actualPrice) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(Long orderId, Long passengerId, Integer fromStatus, Long driverId,
                               Integer operator, String reason) {

        if (orderMapper.cancel(orderId) == 0) {
            log.warn("订单 {} 取消失败：当前状态不可取消（期望 {}）", orderId, fromStatus);
            return false;
        }

        orderDetailMapper.freeze(orderId);

        if (driverId != null && driverMapper.release(driverId) == 0) {
            log.warn("订单 {} 取消：司机 {} 当前不是接单中，跳过", orderId, driverId);
        }

        writeLog(orderId, fromStatus, OrderStatusEnum.CANCELED.getCode(), operator, passengerId);

        log.info("订单 {} 已取消，原因：{}", orderId, reason);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rollbackForRedispatch(Long orderId, Long passengerId, Long driverId, Integer operator) {

        if (driverId == null || orderMapper.rollbackToWaiting(orderId, driverId) == 0) {
            log.warn("重派补偿跳过：订单 {} 状态已变化", orderId);
            return false;
        }

        if (driverMapper.release(driverId) == 0) {
            log.warn("重派补偿：司机 {} 当前不是接单中", driverId);
        }

        resetDetail(orderId);

        writeLog(orderId, OrderStatusEnum.DISPATCHED.getCode(),
                OrderStatusEnum.WAITING_DISPATCH.getCode(), operator, passengerId);

        return true;
    }

    private void resetDetail(Long orderId) {

        OrderDetail detail = orderDetailMapper.selectByOrderId(orderId);

        if (detail == null) {
            log.warn("重派补偿：订单详情不存在 orderId={}", orderId);
            return;
        }

        if (detail.getActualPrice() != null) {
            log.info("重派补偿跳过：订单已结算 orderId={}", orderId);
            return;
        }

        String minimal = null;
        RouteInfo base = detail.deserializeRouteInfo();

        if (base != null) {
            RouteInfo keep = new RouteInfo();
            keep.setStartAndEnd(base.getStartLng(), base.getStartLat(), base.getEndLng(), base.getEndLat());
            keep.setStartAddress(base.getStartAddress());
            keep.setEndAddress(base.getEndAddress());
            minimal = JSON.toJSONString(keep);
        }

        orderDetailMapper.resetForRedispatch(orderId, minimal);
    }

    private void writeLog(Long orderId, Integer fromStatus, Integer toStatus, Integer operator, Long passengerId) {

        if (orderStatusLogMapper.insert(new OrderStatusLog(orderId, fromStatus, toStatus, operator, passengerId)) == 0) {
            throw new RuntimeException("订单流转记录插入失败");
        }
    }
}