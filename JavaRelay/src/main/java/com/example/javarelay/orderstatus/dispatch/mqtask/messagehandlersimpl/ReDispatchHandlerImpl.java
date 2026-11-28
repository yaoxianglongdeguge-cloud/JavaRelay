package com.example.javarelay.orderstatus.dispatch.mqtask.messagehandlersimpl;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.infra.redis.RedisUtil;
import com.example.javarelay.orderstatus.constants.statusenum.OrderOperatorEnum;
import com.example.javarelay.orderstatus.constants.statusenum.OrderStatusEnum;
import com.example.javarelay.orderstatus.dispatch.mqtask.messagecells.DispatchMessage;
import com.example.javarelay.orderstatus.dispatch.mqtask.messagecells.ReDispatchMessage;
import com.example.javarelay.orderstatus.entity.Order;
import com.example.javarelay.orderstatus.mapper.OrderMapper;
import com.example.javarelay.orderstatus.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(topic = "jr_redispatch", consumerGroup = "jr_redispatch_group")
public class ReDispatchHandlerImpl implements RocketMQListener<String> {

    @Autowired
    OrderService orderService;

    @Autowired
    OrderMapper orderMapper;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @Override
    public void onMessage(String body) {

        ReDispatchMessage cell = JSON.parseObject(body, ReDispatchMessage.class);

        if (cell == null || cell.getMsg() == null) {
            log.warn("重派消息格式错误: {}", body);
            return;
        }

        Long orderId = cell.getMsg().getOrderId();
        Long passengerId = cell.getMsg().getPassengerId();
        Integer operator = cell.getMsg().getOperator() == null
                ? OrderOperatorEnum.SYSTEM.getCode() : cell.getMsg().getOperator();

        Order order = orderMapper.selectById(orderId);

        if (order == null) {
            log.warn("重派跳过：订单不存在 orderId={}", orderId);
            return;
        }

        Long driverId = order.getDriverId();

        boolean rolled = orderService.rollbackForRedispatch(orderId, passengerId, operator);

        if (driverId != null) {
            redisUtil.tryunLockDriver(driverId);
        }

        Order latest = orderMapper.selectById(orderId);

        boolean dispatchable = latest != null
                && latest.getFrozenAt() == null
                && latest.getStatus() != null
                && latest.getStatus().equals(OrderStatusEnum.WAITING_DISPATCH.getCode());

        if (!dispatchable) {
            log.info("重派跳过：订单不在待派单 orderId={} status={}", orderId,
                    latest == null ? null : latest.getStatus());
            return;
        }

        eventPublisher.publishEvent(new DispatchMessage(orderId, passengerId, DispatchMessage.Step.DRIVER));

        log.info("订单 {} 回溯完成（{}），已重新提交派单", orderId, rolled ? "已回溯" : "无需回溯");
    }
}