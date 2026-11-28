package com.example.javarelay.orderstatus.service.Impl;

import com.example.javarelay.orderstatus.entity.Order;
import com.example.javarelay.orderstatus.entity.OrderDetail;
import com.example.javarelay.orderstatus.entity.OrderStatusLog;
import com.example.javarelay.data.mapper.DriverMapper;
import com.example.javarelay.orderstatus.mapper.OrderDetailMapper;
import com.example.javarelay.orderstatus.mapper.OrderMapper;
import com.example.javarelay.orderstatus.node.OrderNodeMapper;
import com.example.javarelay.orderstatus.mapper.OrderStatusLogMapper;
import com.example.javarelay.dto.Result;
import com.example.javarelay.orderstatus.config.StrategyTypeConfig;
import com.example.javarelay.orderstatus.constants.Constants;
import com.example.javarelay.orderstatus.constants.statusenum.OrderOperatorEnum;
import com.example.javarelay.orderstatus.constants.statusenum.OrderStatusEnum;
import com.example.javarelay.orderstatus.node.OrderNode;
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
import java.util.ArrayList;
import java.util.List;

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

    @Autowired
    OrderNodeMapper orderNodeMapper;

    private enum NodeResult {
        EXECUTED, ALREADY_DONE, NOT_READY
    }

    private NodeResult advance(Long orderId, OrderNode node) {

        // ① 串行点：锁住订单行，同一订单的节点推进从这里排队
        Order order = orderMapper.lockOrder(orderId);

        if (order == null) {
            log.warn("订单不存在 orderId={}", orderId);
            return NodeResult.NOT_READY;
        }
        if (order.getFrozenAt() != null) {
            return NodeResult.NOT_READY;
        }

        // ② 已被串行化，此时节点集合不会再变
        List<String> done = orderNodeMapper.selectKeys(orderId);

        if (done.contains(node.name())) {
            return NodeResult.ALREADY_DONE;
        }

        for (OrderNode p : node.directPre()) {
            if (!done.contains(p.name())) {
                log.info("节点未就绪 orderId={} node={} 缺前置={}", orderId, node, p);
                return NodeResult.NOT_READY;
            }
        }

        // ③ 写入（主键兜底）
        try {
            orderNodeMapper.insertNode(orderId, node.name());
        } catch (DuplicateKeyException e) {
            return NodeResult.ALREADY_DONE;
        }

        return NodeResult.EXECUTED;
    }

    private void writeLog(Long orderId, Integer from, Integer to, Integer operator, Long passengerId) {
        if (orderStatusLogMapper.insert(
                new OrderStatusLog(orderId, from, to, operator, passengerId)) == 0) {
            throw new RuntimeException("订单流转记录插入失败");
        }
    }

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

        OrderDetail detail = new OrderDetail();
        detail.setOrderId(order.getId());
        detail.setPassengerId(passengerId);
        detail.setAcpriceType(passengerDto.getComputeMoneyType());
        detail.setEspriceType(StrategyTypeConfig.PRECOMPUTE_MONEY_TYPE);
        detail.setDriverType(passengerDto.getSelectDriverStrategy());
        detail.setPassengerType(StrategyTypeConfig.PASSENGER_SELECT_TYPE);
        detail.setPathType(StrategyTypeConfig.PATH_SELECT_TYPE);

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

        if (advance(order.getId(), OrderNode.CREATE) != NodeResult.EXECUTED) {
            throw new StatusChangeException("NODE_CREATE_FAIL");
        }

        writeLog(order.getId(), OrderStatusEnum.WAITING_BIRTH.getCode(),
                OrderStatusEnum.WAITING_DISPATCH.getCode(), OrderOperatorEnum.PASSENGER.getCode(), passengerId);

        return Result.success(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean dispatchOrder(Long orderId, Long passengerId, Long driverId) {

        NodeResult r = advance(orderId, OrderNode.DISPATCH);
        if (r == NodeResult.NOT_READY) {
            return false;
        }
        if (r == NodeResult.ALREADY_DONE) {
            return true;
        }

        orderMapper.markDispatched(orderId, driverId);

        if (driverMapper.occupy(driverId, orderId) == 0) {
            throw new StatusChangeException("DRIVER_BUSY");
        }

        writeLog(orderId, OrderStatusEnum.WAITING_DISPATCH.getCode(),
                OrderStatusEnum.DISPATCHED.getCode(), OrderOperatorEnum.PASSENGER.getCode(), passengerId);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveRouteAndPrice(Long orderId, Long driverId,
                                     BigDecimal estimatedPrice, String routeSnapshot) {

        NodeResult r = advance(orderId, OrderNode.ROUTE_PRICE);
        if (r == NodeResult.NOT_READY) {
            return false;
        }
        if (r == NodeResult.ALREADY_DONE) {
            return true;
        }

        orderDetailMapper.markRouteAndPrice(orderId, estimatedPrice, routeSnapshot);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean receiveOrder(Long orderId, Long passengerId, Long driverId) {

        NodeResult r = advance(orderId, OrderNode.RECEIVE);
        if (r == NodeResult.NOT_READY) {
            return false;
        }
        if (r == NodeResult.ALREADY_DONE) {
            return true;
        }

        orderMapper.markReceived(orderId);

        writeLog(orderId, OrderStatusEnum.DISPATCHED.getCode(),
                OrderStatusEnum.RECEIVED.getCode(), OrderOperatorEnum.DRIVER.getCode(), passengerId);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean arriveOrder(Long orderId, Long passengerId, Long driverId) {

        NodeResult r = advance(orderId, OrderNode.ARRIVE);
        if (r == NodeResult.NOT_READY) {
            return false;
        }
        if (r == NodeResult.ALREADY_DONE) {
            return true;
        }

        orderMapper.markArrived(orderId);

        writeLog(orderId, OrderStatusEnum.RECEIVED.getCode(),
                OrderStatusEnum.ARRIVED.getCode(), OrderOperatorEnum.DRIVER.getCode(), passengerId);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean finishOrder(Long orderId, Long passengerId, Long driverId) {

        NodeResult r = advance(orderId, OrderNode.FINISH);
        if (r == NodeResult.NOT_READY) {
            return false;
        }
        if (r == NodeResult.ALREADY_DONE) {
            return true;
        }

        orderMapper.markFinished(orderId);
        driverMapper.releaseByOrder(orderId);

        writeLog(orderId, OrderStatusEnum.ARRIVED.getCode(),
                OrderStatusEnum.FINISHED.getCode(), OrderOperatorEnum.DRIVER.getCode(), passengerId);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean settleOrder(Long orderId, BigDecimal actualPrice) {

        NodeResult r = advance(orderId, OrderNode.SETTLE);
        if (r == NodeResult.NOT_READY) {
            return false;
        }
        if (r == NodeResult.ALREADY_DONE) {
            return true;
        }

        orderDetailMapper.markSettled(orderId, actualPrice);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(Long orderId, Long passengerId, Integer fromStatus, Long driverId,
                               Integer operator, String reason) {

        NodeResult r = advance(orderId, OrderNode.FROZEN);
        if (r == NodeResult.NOT_READY) {
            log.warn("订单 {} 冻结失败：节点未就绪", orderId);
            return false;
        }
        if (r == NodeResult.ALREADY_DONE) {
            return true;
        }

        orderMapper.markFrozen(orderId);
        driverMapper.releaseByOrder(orderId);

        writeLog(orderId, fromStatus, OrderStatusEnum.CANCELED.getCode(), operator, passengerId);

        log.info("订单 {} 已冻结，原因：{}", orderId, reason);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rollbackForRedispatch(Long orderId, Long passengerId, Long driverId, Integer operator) {

        if (orderNodeMapper.count(orderId, OrderNode.DISPATCH.name()) == 0) {
            log.warn("重派补偿跳过：订单 {} 没有 DISPATCH 节点", orderId);
            return false;
        }

        List<String> keys = new ArrayList<>();
        keys.add(OrderNode.DISPATCH.name());
        OrderNode.DISPATCH.descendants().forEach(n -> keys.add(n.name()));

        orderNodeMapper.deleteMany(orderId, keys);

        orderMapper.rollbackToWaiting(orderId);
        orderDetailMapper.clearActualPrice(orderId);
        driverMapper.releaseByOrder(orderId);

        writeLog(orderId, OrderStatusEnum.DISPATCHED.getCode(),
                OrderStatusEnum.WAITING_DISPATCH.getCode(), operator, passengerId);

        return true;
    }
}
