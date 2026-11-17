package com.example.javarelay.messagetask.MessageCells;

import com.example.javarelay.messagetask.MessageCell;
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
        this.setMsg(new ReDispatchMessage.msg(orderId,passengerId));
    }

}
