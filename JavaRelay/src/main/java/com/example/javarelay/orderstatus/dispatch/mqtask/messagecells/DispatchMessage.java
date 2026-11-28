package com.example.javarelay.orderstatus.dispatch.mqtask.messagecells;

import com.example.javarelay.infra.mq.MessageCell;
import com.example.javarelay.orderstatus.config.MQConfig;
import com.example.javarelay.orderstatus.dispatch.dto.DispatchData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class DispatchMessage extends MessageCell<DispatchMessage.msg> {

    public enum Step {

        PASSENGER,

        DRIVER,

        PATH;

        public Step next() {
            return switch (this) {
                case PASSENGER -> DRIVER;
                case DRIVER -> PATH;
                case PATH -> null;
            };
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class msg {

        Long orderId;

        Long passengerId;

        Step step;

        // false = 处理前（只算）；true = 处理后（只写库）
        boolean done;

        DispatchData data;
    }

    public DispatchMessage() {
    }

    public DispatchMessage(Long orderId, Long passengerId, Step step) {
        this(orderId, passengerId, step, false, null);
    }

    public DispatchMessage(Long orderId, Long passengerId, Step step, boolean done, DispatchData data) {
        this.setTopic(MQConfig.MESSAGE_DISPATCH_TOPIC);
        this.setMsg(new msg(orderId, passengerId, step, done, data));
    }
}