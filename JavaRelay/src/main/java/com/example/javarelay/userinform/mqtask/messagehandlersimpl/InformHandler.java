package com.example.javarelay.userinform.mqtask.messagehandlersimpl;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.globalconfig.RootInformConfig;
import com.example.javarelay.orderstatus.constants.Constants;
import com.example.javarelay.orderstatus.mqtask.messagecells.OrderTimeoutMessage;
import com.example.javarelay.userinform.constants.InformTypeEnum;
import com.example.javarelay.userinform.infra.websocket.MyWebSocketHandler;
import com.example.javarelay.userinform.mqtask.messagecells.InformMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(topic = "jr_inform", consumerGroup = "jr_inform_group")
public class InformHandler implements RocketMQListener<String> {

    @Autowired
    MyWebSocketHandler webSocketHandler;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @Override
    public void onMessage(String body) {

        InformMessage cell = JSON.parseObject(body, InformMessage.class);

        if (cell == null || cell.getMsg() == null) {
            log.warn("通知消息格式错误: {}", body);
            return;
        }

        handle(cell);
    }

    public void handle(InformMessage informMessage) {

        InformMessage.msg msg = informMessage.getMsg();
        Integer toWho = msg.getToWho();
        Integer type = msg.getMsgType();
        String message = msg.getMessage();
        Long orderId = msg.getOrderId();

        if (type == InformTypeEnum.STATUS_CONFIRM.getCode()) {

            informStatusConfirm(toWho, msg.getToId(), message);

            if (toWho.equals(RootInformConfig.TO_DRIVER) && "已派单".equals(message)) {
                OrderTimeoutMessage timeout =
                        new OrderTimeoutMessage(orderId, Constants.ORDER_TIMEOUT_TIME);
                eventPublisher.publishEvent(timeout);
            }
        }
    }

    public void informStatusConfirm(Integer toWho, Long toId, String message) {
        webSocketHandler.sendToUser(toWho, toId, message);
    }
}