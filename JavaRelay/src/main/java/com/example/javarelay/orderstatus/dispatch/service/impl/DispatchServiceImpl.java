package com.example.javarelay.orderstatus.dispatch.service.impl;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.dto.Result;
import com.example.javarelay.infra.redis.RedisUtil;
import com.example.javarelay.orderstatus.constants.statusenum.OrderOperatorEnum;
import com.example.javarelay.orderstatus.constants.statusenum.OrderStatusEnum;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.mqtask.messagecells.DriverMatchMessage;
import com.example.javarelay.orderstatus.dispatch.dto.DispatchData;
import com.example.javarelay.orderstatus.dispatch.mqtask.messagecells.DispatchMessage;
import com.example.javarelay.orderstatus.dispatch.service.DispatchService;
import com.example.javarelay.orderstatus.entity.Order;
import com.example.javarelay.orderstatus.entity.OrderDetail;
import com.example.javarelay.orderstatus.exception.StatusChangeException;
import com.example.javarelay.orderstatus.mapper.OrderDetailMapper;
import com.example.javarelay.orderstatus.mapper.OrderMapper;
import com.example.javarelay.orderstatus.pojo.RouteInfo;
import com.example.javarelay.orderstatus.service.OrderService;
import com.example.javarelay.orderstatus.service.StrategysService;
import com.example.javarelay.userinform.constants.InformTypeEnum;
import com.example.javarelay.userinform.service.InformService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class DispatchServiceImpl implements DispatchService {

    @Autowired
    OrderMapper orderMapper;

    @Autowired
    OrderDetailMapper orderDetailMapper;

    @Autowired
    OrderService orderService;

    @Autowired
    StrategysService strategysService;

    @Autowired
    InformService informService;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @Override
    public DispatchData prepare(DispatchMessage.Step step, Long orderId, Long passengerId) {
        return switch (step) {
            case PASSENGER -> preparePassenger(orderId, passengerId);
            case DRIVER -> prepareDriver(orderId, passengerId);
            case PATH -> preparePath(orderId, passengerId);
        };
    }

    @Override
    public Result commit(DispatchMessage.Step step, Long orderId, Long passengerId, DispatchData data) {
        return switch (step) {
            case PASSENGER -> commitPassenger(orderId, passengerId, data);
            case DRIVER -> commitDriver(orderId, passengerId, data);
            case PATH -> commitPath(orderId, passengerId, data);
        };
    }

    private DispatchData preparePassenger(Long orderId, Long passengerId) {

        DispatchData data = new DispatchData();

        Order order;
        try {
            order = orderMapper.passengerSelectUnFinshed(passengerId);
        } catch (Exception e) {
            data.setFailReason("系统问题");
            return data;
        }

        if (order == null || order.getStatus() == null
                || order.getStatus() != OrderStatusEnum.WAITING_DISPATCH.getCode()) {
            data.setFailReason("不存在需要派的订单");
            return data;
        }

        Result result = strategysService.findPassengerByStrategy(orderId);

        if (result == null || result.getCode() != 200) {
            data.setFailReason("用户不符合打车条件");
        }

        return data;
    }

    private DispatchData prepareDriver(Long orderId, Long passengerId) {

        Order order = orderMapper.selectById(orderId);

        if (order == null) {
            return null;
        }
        if (order.getStatus() == null
                || order.getStatus() != OrderStatusEnum.WAITING_DISPATCH.getCode()) {
            return null;
        }

        eventPublisher.publishEvent(new DriverMatchMessage(orderId, passengerId));

        return null;
    }

    private DispatchData preparePath(Long orderId, Long passengerId) {

        DispatchData data = new DispatchData();

        Order order = orderMapper.selectById(orderId);

        if (order == null) {
            return null;
        }
        if (order.getStatus() == null
                || order.getStatus() != OrderStatusEnum.DISPATCHED.getCode()) {
            return null;
        }
        if (order.getDriverId() == null) {
            return null;
        }

        OrderDetail orderDetail = orderDetailMapper.selectByOrderId(orderId);

        if (orderDetail == null) {
            data.setFailReason("订单详情不存在");
            return data;
        }
        if (orderDetail.getEstimatedPrice() != null) {
            return null;
        }

        RouteInfo base = orderDetail.deserializeRouteInfo();

        if (base == null) {
            data.setFailReason("路线快照缺失");
            return data;
        }

        Result resultPath = strategysService.findPathByStrategy(orderId);

        if (resultPath == null || resultPath.getCode() != 200) {
            data.setFailReason("路线计算失败");
            return data;
        }

        RouteInfo routeInfo = (RouteInfo) resultPath.getData();
        routeInfo.setStartAndEnd(base.getStartLng(), base.getStartLat(), base.getEndLng(), base.getEndLat());
        routeInfo.setStartAddress(base.getStartAddress());
        routeInfo.setEndAddress(base.getEndAddress());

        Result resultPreMoney = strategysService.preComputeMoney(orderId);
        BigDecimal preMoney = resultPreMoney == null ? null : (BigDecimal) resultPreMoney.getData();

        if (preMoney == null) {
            data.setFailReason("预估价计算失败");
            return data;
        }

        data.setRoute(routeInfo);
        data.setEstimatedPrice(preMoney);

        return data;
    }

    private Result commitPassenger(Long orderId, Long passengerId, DispatchData data) {

        if (data == null || data.getFailReason() == null) {
            return Result.success("乘客校验通过");
        }

        boolean canceled = orderService.cancelOrder(orderId, passengerId,
                OrderStatusEnum.WAITING_DISPATCH.getCode(), null,
                OrderOperatorEnum.SYSTEM.getCode(), data.getFailReason());

        if (canceled) {
            informService.informPassenger(orderId, null, passengerId,
                    InformTypeEnum.STATUS_CONFIRM.getCode(), "订单已取消：" + data.getFailReason());
        }

        return Result.fail(data.getFailReason());
    }

    private Result commitDriver(Long orderId, Long passengerId, DispatchData data) {

        if (data == null || data.getFailReason() != null) {
            return Result.fail(data == null ? "没有派单数据" : data.getFailReason());
        }

        Long driverId = data.getDriverId();

        if (driverId == null) {
            return Result.fail("司机为空");
        }

        if (!redisUtil.tryLockDriver(driverId)) {
            return Result.fail("司机已被占用");
        }

        try {
            if (!orderService.dispatchOrder(orderId, passengerId, driverId)) {
                throw new StatusChangeException("ORDER_CHANGE_FAIL");
            }
        } catch (StatusChangeException e) {
            redisUtil.tryunLockDriver(driverId);
            return Result.fail("派单状态变更失败: " + e.getMessage());
        } catch (Exception e) {
            redisUtil.tryunLockDriver(driverId);
            log.error("派单落库失败 orderId={} driverId={}", orderId, driverId, e);
            return Result.fail("系统出问题");
        }

        return Result.success("派单成功");
    }

    private Result commitPath(Long orderId, Long passengerId, DispatchData data) {

        if (data == null || data.getFailReason() != null) {
            return Result.fail(data == null ? "没有路线数据" : data.getFailReason());
        }

        Order order = orderMapper.selectById(orderId);

        if (order == null || order.getDriverId() == null) {
            return Result.fail("订单没有司机");
        }

        if (!orderService.saveRouteAndPrice(orderId, order.getDriverId(),
                data.getEstimatedPrice(), JSON.toJSONString(data.getRoute()))) {
            return Result.success("路线已由其他链路写入，跳过");
        }

        informService.informDriver(orderId, passengerId, order.getDriverId(),
                InformTypeEnum.STATUS_CONFIRM.getCode(), "已派单");

        return Result.success("路线与预估完成");
    }
}