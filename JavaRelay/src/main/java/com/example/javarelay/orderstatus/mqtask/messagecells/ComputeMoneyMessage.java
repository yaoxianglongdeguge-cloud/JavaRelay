package com.example.javarelay.orderstatus.mqtask.messagecells;

import com.example.javarelay.infra.mq.MessageCell;
import com.example.javarelay.orderstatus.config.MQConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class ComputeMoneyMessage extends MessageCell<ComputeMoneyMessage.msg> {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class msg {
        Long orderId;
    }

    public ComputeMoneyMessage() {
    }

    public ComputeMoneyMessage(Long orderId) {
        this.setTopic(MQConfig.MESSAGE_COMPUTEMONEY_TOPIC);
        this.setMsg(new msg(orderId));
    }

}