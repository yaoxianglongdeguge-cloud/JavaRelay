package com.example.javarelay.userinform.mqtask.messagecells;

import com.example.javarelay.infra.mq.MessageCell;
import com.example.javarelay.userinform.config.MessageTaskConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class InformMessage extends MessageCell<InformMessage.msg> {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class msg {

        Integer toWho;
        Long orderId;
        Long meId;
        Long toId;
        Integer msgType;
        String message;
    }

    public InformMessage() {
    }

    public InformMessage(Integer toWho, Long orderId, Long meId, Long toId, Integer msgType, String message) {
        this.setTopic(MessageTaskConfig.MESSAGE_INFORM_TOPIC);
        this.setMsg(new msg(toWho, orderId, meId, toId, msgType, message));
    }
}