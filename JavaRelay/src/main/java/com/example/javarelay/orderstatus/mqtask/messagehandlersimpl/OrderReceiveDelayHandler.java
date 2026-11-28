package com.example.javarelay.orderstatus.mqtask.messagehandlersimpl;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.orderstatus.constants.statusenum.OrderOperatorEnum;
import com.example.javarelay.orderstatus.constants.statusenum.OrderStatusEnum;
import com.example.javarelay.orderstatus.dispatch.mqtask.messagecells.ReDispatchMessage;
import com.example.javarelay.orderstatus.entity.Order;
import com.example.javarelay.orderstatus.mapper.OrderMapper;
import com.example.javarelay.orderstatus.mqtask.messagecells.OrderTimeoutMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(topic = "jr_delay_receive", consumerGroup = "jr_delay_receive_group")
public class OrderReceiveDelayHandler implements RocketMQListener<String> {

    @Autowired
    OrderMapper orderMapper;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @Override
    public void onMessage(String body) {

        OrderTimeoutMessage cell = JSON.parseObject(body, OrderTimeoutMessage.class);

        if (cell == null || cell.getMsg() == null) {
            log.warn("接单超时消息格式错误: {}", body);
            return;
        }

        handleTimeout(cell.getMsg().getOrderId());
    }

    private void handleTimeout(Long orderId) {

        Order order;

        try {
            order = orderMapper.selectById(orderId);
        } catch (Exception e) {
            log.error("查询订单失败 orderId={}", orderId, e);
            return;
        }

        if (order == null) {
            return;
        }

        if (order.getStatus() == null
                || order.getStatus() != OrderStatusEnum.DISPATCHED.getCode()) {
            return;
        }

        eventPublisher.publishEvent(new ReDispatchMessage(orderId, order.getPassengerId(),
                OrderOperatorEnum.SYSTEM.getCode()));

        log.info("订单 {} 接单超时，已提交重派", orderId);
    }
}