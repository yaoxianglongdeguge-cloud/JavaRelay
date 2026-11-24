package com.example.javarelay.orderstatus.mqtask.messagecells;

import com.example.javarelay.infra.mq.MessageCell;
import com.example.javarelay.orderstatus.config.MQConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 首次派单入口消息：订单创建后发出，由它启动后续的异步处理链。
 * 与 ReDispatchMessage 的区别：本消息只用于"第一次"，重试一律走 ReDispatch。
 */
public class DispatchMessage extends MessageCell<DispatchMessage.msg> {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class msg {

        Long orderId;
        Long passengerId;
    }

    public DispatchMessage() {
    }

    public DispatchMessage(Long orderId, Long passengerId) {
        this.setTopic(MQConfig.MESSAGE_DISPATCH_TOPIC);
        this.setMsg(new msg(orderId, passengerId));
    }
}