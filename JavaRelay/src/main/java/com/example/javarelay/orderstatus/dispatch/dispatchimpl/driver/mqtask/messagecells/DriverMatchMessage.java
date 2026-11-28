package com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.mqtask.messagecells;

import com.example.javarelay.infra.mq.MessageCell;
import com.example.javarelay.orderstatus.config.MQConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class DriverMatchMessage extends MessageCell<DriverMatchMessage.msg> {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class msg {

        Long orderId;

        Long passengerId;
    }

    public DriverMatchMessage() {
    }

    public DriverMatchMessage(Long orderId, Long passengerId) {
        this.setTopic(MQConfig.MESSAGE_MATCH_DRIVER_TOPIC);
        this.setMsg(new msg(orderId, passengerId));
    }
}