package com.example.javarelay.orderstatus.mqtask.messagehandlersimpl;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.orderstatus.entity.Order;
import com.example.javarelay.dto.Result;
import com.example.javarelay.orderstatus.mapper.OrderMapper;
import com.example.javarelay.orderstatus.constants.statusenum.OrderOperatorEnum;
import com.example.javarelay.orderstatus.constants.statusenum.OrderStatusEnum;
import com.example.javarelay.orderstatus.exception.StatusChangeException;
import com.example.javarelay.orderstatus.mqtask.messagecells.OrderTimeoutMessage;
import com.example.javarelay.orderstatus.service.OrderService;
import com.example.javarelay.orderstatus.service.PassengerOperateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(topic = "jr_delay_receive", consumerGroup = "jr_delay_receive_group")
public class OrderReceiveDelayHandler implements RocketMQListener<String> {

    @Autowired
    OrderMapper orderMapper;

    @Autowired
    OrderService orderService;

    @Autowired
    PassengerOperateService passengerOperateService;

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

        Integer status = order.getStatus();
        Long passengerId = order.getPassengerId();
        Long driverId = order.getDriverId();

        if (status == null || status != OrderStatusEnum.DISPATCHED.getCode()) {
            return;
        }

        try {
            if (!orderService.rollbackForRedispatch(orderId, passengerId, driverId,
                    OrderOperatorEnum.SYSTEM.getCode())) {
                return;
            }

            Result redispatch = passengerOperateService.ReDispatchOrder(orderId, passengerId);

            if (redispatch == null || redispatch.getCode() != 200) {
                log.warn("接单超时重派失败 orderId={} reason={}", orderId,
                        redispatch == null ? "null" : redispatch.getMessage());
            } else {
                log.info("订单 {} 接单超时，已提交匹配司机请求", orderId);
            }

        } catch (StatusChangeException e) {
            log.info("订单 {} 已被取消或接单，跳过超时处理", orderId);
        }
    }
}
