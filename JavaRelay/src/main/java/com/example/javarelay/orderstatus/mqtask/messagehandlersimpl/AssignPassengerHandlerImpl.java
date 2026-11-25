package com.example.javarelay.orderstatus.mqtask.messagehandlersimpl;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.orderstatus.mqtask.messagecells.AssignPassengerMessage;
import com.example.javarelay.orderstatus.service.DispatchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(topic = "jr_assignPassenger", consumerGroup = "jr_assignPassenger_group")
public class AssignPassengerHandlerImpl implements RocketMQListener<String> {

    @Autowired
    DispatchService dispatchService;

    @Override
    public void onMessage(String body) {

        AssignPassengerMessage cell = JSON.parseObject(body, AssignPassengerMessage.class);

        if (cell == null || cell.getMsg() == null) {
            log.warn("乘客校验消息格式错误: {}", body);
            return;
        }

        Long orderId = cell.getMsg().getOrderId();
        Long passengerId = cell.getMsg().getPassengerId();

        dispatchService.AssignPassenger(orderId, passengerId);
    }
}
