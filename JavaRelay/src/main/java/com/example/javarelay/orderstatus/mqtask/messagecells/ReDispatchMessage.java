package com.example.javarelay.orderstatus.mqtask.messagecells;

import com.example.javarelay.infra.mq.MessageCell;
import com.example.javarelay.orderstatus.config.MQConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 重派消息：订单"提交分配"失败后的重试载体。
 *
 * <p>{@code retryCount} 让重试次数随消息传递，不需要额外的存储。
 */
public class ReDispatchMessage extends MessageCell<ReDispatchMessage.msg> {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class msg {

        Long orderId;

        Long passengerId;

        /** 已经重试过的次数，首次为 0 */
        int retryCount;
    }

    public ReDispatchMessage() {
    }

    public ReDispatchMessage(Long orderId, Long passengerId) {
        this.setTopic(MQConfig.MESSAGE_REDISPATCH_TOPIC);
        this.setMsg(new msg(orderId, passengerId, 0));
    }

    public ReDispatchMessage(Long orderId, Long passengerId, int retryCount) {
        this.setTopic(MQConfig.MESSAGE_REDISPATCH_TOPIC);
        this.setMsg(new msg(orderId, passengerId, retryCount));
    }
}