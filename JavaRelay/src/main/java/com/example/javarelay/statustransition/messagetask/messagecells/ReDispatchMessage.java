package com.example.javarelay.statustransition.messagetask.messagecells;

import com.example.javarelay.infra.messagetask.MessageCell;
import com.example.javarelay.statustransition.config.MessageTaskConfig;
import lombok.Data;

public class ReDispatchMessage extends MessageCell {

    @Data
    public class msg{

        Long orderId;
        Long passengerId;

        public msg(Long orderId,Long passengerId){

            this.orderId = orderId;
            this.passengerId = passengerId;
        }
    }

    public ReDispatchMessage(Long orderId,Long passengerId){
        this.setTopic(MessageTaskConfig.MESSAGE_REDISPATCH_TOPIC);
        this.setMsg(new ReDispatchMessage.msg(orderId,passengerId));
    }

}
