package com.example.javarelay.orderstatus.mqtask.messagecells;

import com.example.javarelay.infra.mq.MessageCell;
import com.example.javarelay.orderstatus.config.MQConfig;
import lombok.Data;

@Data
public class ComputeMoneyMessage extends MessageCell {

    @Data
    public class msg{
        Long orderId;

        public msg(Long orderId){
            this.orderId = orderId;
        }
    }

    public ComputeMoneyMessage(Long orderId){
        this.setTopic(MQConfig.MESSAGE_COMPUTEMONEY_TOPIC);
        this.setMsg(new msg(orderId));
    }

}
