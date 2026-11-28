package com.example.javarelay.orderstatus.dispatch.mqtask.messagecells;

import com.example.javarelay.infra.mq.MessageCell;
import com.example.javarelay.orderstatus.config.MQConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 重派消息：让订单"再提交一次分配请求"。
 *
 * <p>失败处理已收进消费者（记日志），不再携带重试次数。
 */
public class ReDispatchMessage extends MessageCell<ReDispatchMessage.msg> {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class msg {

        Long orderId;

        Long passengerId;
    }

    public ReDispatchMessage() {
    }

    public ReDispatchMessage(Long orderId, Long passengerId) {
        this.setTopic(MQConfig.MESSAGE_REDISPATCH_TOPIC);
        this.setMsg(new msg(orderId, passengerId));
    }
}