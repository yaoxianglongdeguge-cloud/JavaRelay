package com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.mqtask.messagehandlersimpl;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.orderstatus.constants.statusenum.OrderOperatorEnum;
import com.example.javarelay.orderstatus.constants.statusenum.OrderStatusEnum;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.mqtask.messagecells.DriverMatchTimeoutMessage;
import com.example.javarelay.orderstatus.service.OrderService;
import com.example.javarelay.userinform.constants.InformTypeEnum;
import com.example.javarelay.userinform.service.InformService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(topic = "jr_match_timeout", consumerGroup = "jr_match_timeout_group")
public class DriverMatchTimeoutHandlerImpl implements RocketMQListener<String> {

    @Autowired
    OrderService orderService;

    @Autowired
    InformService informService;

    @Override
    public void onMessage(String body) {

        DriverMatchTimeoutMessage cell = JSON.parseObject(body, DriverMatchTimeoutMessage.class);

        if (cell == null || cell.getMsg() == null) {
            log.warn("派司机超时消息格式错误: {}", body);
            return;
        }

        Long orderId = cell.getMsg().getOrderId();
        Long passengerId = cell.getMsg().getPassengerId();

        boolean canceled = orderService.cancelOrder(orderId, passengerId,
                OrderStatusEnum.WAITING_DISPATCH.getCode(), null,
                OrderOperatorEnum.SYSTEM.getCode(), "附近无可用司机");

        if (!canceled) {
            log.info("派司机超时，订单 {} 状态已变，跳过", orderId);
            return;
        }

        informService.informPassenger(orderId, null, passengerId,
                InformTypeEnum.STATUS_CONFIRM.getCode(), "订单已取消：附近无可用司机");

        log.info("订单 {} 派司机超时，已取消", orderId);
    }
}