package com.example.javarelay.orderstatus.mqtask.messagecells;

import com.example.javarelay.infra.mq.MessageCell;
import com.example.javarelay.orderstatus.config.MQConfig;
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
        this.setTopic(MQConfig.MESSAGE_REDISPATCH_TOPIC);
        this.setMsg(new ReDispatchMessage.msg(orderId,passengerId));
    }

}
