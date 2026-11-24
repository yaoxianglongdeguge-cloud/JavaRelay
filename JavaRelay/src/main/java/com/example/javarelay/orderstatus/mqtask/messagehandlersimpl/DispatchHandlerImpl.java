package com.example.javarelay.orderstatus.mqtask.messagehandlersimpl;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.orderstatus.mqtask.messagecells.AssignPassengerMessage;
import com.example.javarelay.orderstatus.mqtask.messagecells.DispatchMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 派单入口：只负责把订单推进到下一步（乘客校验）。
 * 后续如果需要"路线与派单并行"，可以在这里额外投递 AssignPathMessage。
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "jr_dispatch", consumerGroup = "jr_dispatch_group")
public class DispatchHandlerImpl implements RocketMQListener<String> {

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @Override
    public void onMessage(String body) {

        DispatchMessage cell = JSON.parseObject(body, DispatchMessage.class);

        if (cell == null || cell.getMsg() == null) {
            log.warn("派单入口消息格式错误: {}", body);
            return;
        }

        Long orderId = cell.getMsg().getOrderId();
        Long passengerId = cell.getMsg().getPassengerId();

        // 入口 → 乘客校验
        eventPublisher.publishEvent(new AssignPassengerMessage(orderId, passengerId));
    }
}