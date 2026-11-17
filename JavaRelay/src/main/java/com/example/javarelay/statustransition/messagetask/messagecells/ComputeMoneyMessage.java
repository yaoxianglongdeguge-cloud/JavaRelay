package com.example.javarelay.statustransition.messagetask.messagecells;

import com.example.javarelay.infra.messagetask.MessageCell;
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
        this.setMsg(new msg(orderId));
    }

}
