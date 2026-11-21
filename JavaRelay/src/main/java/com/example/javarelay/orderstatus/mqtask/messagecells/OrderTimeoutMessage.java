package com.example.javarelay.orderstatus.mqtask.messagecells;

import com.example.javarelay.infra.mq.MessageCell;
import com.example.javarelay.orderstatus.config.MQConfig;
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
        this.setTopic(MQConfig.ORDER_TIMEOUT_TOPIC);
        this.setMsg(new msg(orderId));
        this.setExecuteTime(System.currentTimeMillis() + delaySeconds * 1000);
    }

}