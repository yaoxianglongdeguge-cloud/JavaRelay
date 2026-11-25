package com.example.javarelay.orderstatus.service.Impl;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.data.entity.Order;
import com.example.javarelay.data.entity.OrderDetail;
import com.example.javarelay.data.mapper.OrderDetailMapper;
import com.example.javarelay.data.mapper.OrderMapper;
import com.example.javarelay.dto.Result;
import com.example.javarelay.infra.redis.RedisUtil;
import com.example.javarelay.orderstatus.constants.statusenum.OrderOperatorEnum;
import com.example.javarelay.orderstatus.constants.statusenum.OrderStatusEnum;
import com.example.javarelay.orderstatus.exception.StatusChangeException;
import com.example.javarelay.orderstatus.mqtask.messagecells.DispatchMessage;

import com.example.javarelay.orderstatus.pojo.RouteInfo;
import com.example.javarelay.orderstatus.service.DispatchService;
import com.example.javarelay.orderstatus.service.OrderService;
import com.example.javarelay.orderstatus.service.StrategysService;
import com.example.javarelay.userinform.constants.InformTypeEnum;
import com.example.javarelay.userinform.service.InformService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 派单流程服务：乘客校验 -> 选司机 -> 落库 -> 路线预估。
 */
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
    public Result AssignPassenger(Long orderId, Long passengerId) {

        Order order;

        try {
            order = orderMapper.passengerSelectUnFinshed(passengerId);
        } catch (Exception e) {
            orderService.cancelOrder(orderId, passengerId,
                    OrderStatusEnum.WAITING_DISPATCH.getCode(), null,
                    OrderOperatorEnum.SYSTEM.getCode(), "系统问题");
            return Result.fail("系统出问题");
        }

        if (order == null || !OrderStatusEnum.of(order.getStatus()).getDesc().equals("待派单")) {
            return Result.fail("不存在需要派的订单");
        }

        // 确定用户是否满足打车条件
        Result result = strategysService.findPassengerByStrategy(orderId);

        if (result == null || result.getCode() != 200) {
            orderService.cancelOrder(orderId, passengerId,
                    OrderStatusEnum.WAITING_DISPATCH.getCode(), null,
                    OrderOperatorEnum.SYSTEM.getCode(), "用户不符合打车条件");
            return result == null ? Result.fail("乘客校验失败") : result;
        }

        // 校验通过 → 投递派单消息
        eventPublisher.publishEvent(new DispatchMessage(orderId, passengerId, DispatchMessage.Step.DRIVER));

        return Result.success("开始寻找司机");
    }

    @Override
    public Result AssignDriver(Long orderId, Long passengerId) {

        Order order;

        try {
            order = orderMapper.selectById(orderId);
        } catch (Exception e) {
            return Result.fail("系统出问题");
        }

        if (order == null) {
            return Result.fail("订单不存在");
        }

        if (order.getStatus() == null
                || !order.getStatus().equals(OrderStatusEnum.WAITING_DISPATCH.getCode())) {
            return Result.success("订单不需要派单");
        }

        Result picked;

        try {
            picked = strategysService.findDriverByStrategy(orderId);
        } catch (Exception e) {
            log.error("选车策略执行失败 orderId={}", orderId, e);
            return Result.fail("系统出问题");
        }

        if (picked == null || picked.getCode() != 200) {
            return Result.fail(picked == null ? "选车失败" : picked.getMessage());
        }

        // 语义唯一：策略返回 200 就代表"已入队"，何时派出去由批处理任务决定
        return Result.success("已加入分配队列");
    }

    @Override
    public Result AssignPath(Long orderId, Long passengerId) {

        Order order;
        try {
            order = orderMapper.selectById(orderId);
        } catch (Exception e) {
            return Result.fail("系统出问题");
        }

        if (order == null) {
            return Result.fail("订单不存在");
        }

        // 顺序守卫一：必须已派单
        if (order.getStatus() == null
                || !order.getStatus().equals(OrderStatusEnum.DISPATCHED.getCode())) {
            return Result.success("订单未派单，跳过路线计算");
        }

        // 顺序守卫二：必须有司机。派单落库（orders.driver_id）是路线写入的前置条件，
        // 否则会把 driver_id = null 写进订单详情并锁死。
        if (order.getDriverId() == null) {
            return Result.success("订单没有司机，跳过路线计算");
        }

        OrderDetail orderDetail;
        try {
            orderDetail = orderDetailMapper.selectByOrderId(orderId);
        } catch (Exception e) {
            return Result.fail("系统出问题");
        }

        if (orderDetail == null) {
            return Result.fail("订单详情不存在");
        }

        // 阶段守卫：详见 OrderDetailMapper.stage 注释
        Integer stage = orderDetail.getDataStage();

        if (stage == null) {
            return Result.success("订单详情已冻结（已取消或已结算），跳过路线计算");
        }
        if (stage >= 2) {
            return Result.success("路线与预估已就绪，跳过");
        }
        if (stage != 1) {
            return Result.success("订单详情尚未派单，跳过路线计算");
        }

        RouteInfo base = orderDetail.deserializeRouteInfo();

        if (base == null) {
            return Result.fail("路线快照缺失");
        }

        Result resultPath = strategysService.findPathByStrategy(orderId);

        if (resultPath == null || resultPath.getCode() != 200) {
            return Result.fail("路线计算失败");
        }

        RouteInfo routeInfo = (RouteInfo) resultPath.getData();
        routeInfo.setStartAndEnd(base.getStartLng(), base.getStartLat(), base.getEndLng(), base.getEndLat());
        routeInfo.setStartAddress(base.getStartAddress());
        routeInfo.setEndAddress(base.getEndAddress());

        Result resultPreMoney = strategysService.preComputeMoney(orderId);
        BigDecimal preMoney = resultPreMoney == null ? null : (BigDecimal) resultPreMoney.getData();

        if (preMoney == null) {
            return Result.fail("预估价计算失败");
        }

        if (!orderService.saveRouteAndPrice(
                orderId, order.getDriverId(), preMoney, JSON.toJSONString(routeInfo))) {
            // 已被其他链路写入，属于幂等命中
            return Result.success("路线已由其他链路写入，跳过");
        }

        // 通知司机（只有这里发一次）
        informService.informDriver(orderId, passengerId, order.getDriverId(),
                InformTypeEnum.STATUS_CONFIRM.getCode(), "已派单");

        return Result.success("路线与预估完成");
    }

    @Override
    public Result applyAssign(Long orderId, Long passengerId, Long driverId) {

        if (driverId == null) {
            return Result.fail("司机为空");
        }

        if (!redisUtil.tryLockDriver(driverId)) {
            return Result.fail("司机已被占用");
        }
//        派单数据库修改：
//        订单主表：
//        activeflag=1
//        status=已派单
//        driverId=1(模拟)

//        绑定司机：
//        Id=1
//        status=忙碌
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

        // 派单成功 → 进入路线与预估环节
        eventPublisher.publishEvent(new DispatchMessage(orderId, passengerId, DispatchMessage.Step.PATH));

        return Result.success("派单成功");
    }
}
