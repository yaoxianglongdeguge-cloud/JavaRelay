package com.example.javarelay.orderstatus.mqtask.messagecells;

import com.example.javarelay.infra.mq.MessageCell;
import com.example.javarelay.orderstatus.config.MQConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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