package com.example.javarelay.orderstatus.mqtask.messagecells;

import com.example.javarelay.infra.mq.MessageCell;
import com.example.javarelay.orderstatus.config.MQConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AssignPathMessage extends MessageCell<AssignPathMessage.msg> {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class msg {

        Long orderId;
        Long passengerId;
    }

    public AssignPathMessage() {
    }

    public AssignPathMessage(Long orderId, Long passengerId) {
        this.setTopic(MQConfig.MESSAGE_ASSIGNPATH_TOPIC);
        this.setMsg(new msg(orderId, passengerId));
    }
}