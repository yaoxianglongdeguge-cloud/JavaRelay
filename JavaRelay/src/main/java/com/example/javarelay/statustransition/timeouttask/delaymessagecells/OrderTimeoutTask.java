package com.example.javarelay.statustransition.timeouttask.delaymessagecells;

import com.example.javarelay.infra.timeouttask.DelayMessageCell;
import com.example.javarelay.statustransition.messagetask.messagecells.ComputeMoneyMessage;
import lombok.Data;

@Data
public class OrderTimeoutTask extends DelayMessageCell {

    @Data
    public class msg{
        Long orderId;

        public msg(Long orderId){
            this.orderId = orderId;
        }
    }

    public OrderTimeoutTask(Long orderId, long delaySeconds) {
        this.setMsg(new msg(orderId));
        this.setExecuteTime(System.currentTimeMillis() + delaySeconds * 1000);
    }

}