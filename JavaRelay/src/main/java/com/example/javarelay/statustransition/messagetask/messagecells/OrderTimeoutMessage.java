package com.example.javarelay.statustransition.messagetask.messagecells;

import com.example.javarelay.infra.mq.MessageCell;
import com.example.javarelay.statustransition.config.MessageTaskConfig;
import lombok.Data;

@Data
public class OrderTimeoutMessage extends MessageCell {


    @Data
    public class msg{
        Long orderId;

        public msg(Long orderId){
            this.orderId = orderId;
        }
    }

    public OrderTimeoutMessage(Long orderId, long delaySeconds) {
        this.setTopic(MessageTaskConfig.ORDER_TIMEOUT_TOPIC);
        this.setMsg(new msg(orderId));
        this.setExecuteTime(System.currentTimeMillis() + delaySeconds * 1000);
    }

}