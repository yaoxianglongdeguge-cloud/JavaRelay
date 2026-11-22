package com.example.javarelay.orderstatus.mqtask.messagecells;

import com.example.javarelay.infra.mq.MessageCell;
import com.example.javarelay.orderstatus.config.MQConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class OrderTimeoutMessage extends MessageCell<OrderTimeoutMessage.msg> {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class msg {
        Long orderId;
    }

    public OrderTimeoutMessage() {
    }

    public OrderTimeoutMessage(Long orderId, long delaySeconds) {
        this.setTopic(MQConfig.ORDER_TIMEOUT_TOPIC);
        this.setMsg(new msg(orderId));
        this.setExecuteTime(System.currentTimeMillis() + delaySeconds * 1000);
    }

}