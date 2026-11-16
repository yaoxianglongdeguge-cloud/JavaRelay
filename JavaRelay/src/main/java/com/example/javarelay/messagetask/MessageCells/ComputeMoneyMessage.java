package com.example.javarelay.messagetask.MessageCells;

import com.example.javarelay.infocollect.InfoCollector;
import com.example.javarelay.messagetask.MessageCell;
import com.example.javarelay.strategys.ComputeMoneyStrategy;
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
