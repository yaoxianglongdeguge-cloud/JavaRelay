package com.example.javarelay.inform.massagetask.messagecells;

import com.example.javarelay.infra.messagetask.MessageCell;
import lombok.Data;

@Data
public class InformMessage extends MessageCell {

    @Data
    public class msg{

        Integer toWho;
        Long orderId;
        Long toId;
        Integer msgType;
        String message;

        public msg(Integer toWho,Long orderId,Long toId,Integer msgType,String message){

            this.toWho = toWho;
            this.msgType = msgType;
            this.message = message;
            this.orderId = orderId;
            this.toId = toId;
        }
    }

    public InformMessage(Integer toWho,Long orderId,Long toId,Integer msgType,String message){
        this.setMsg(new InformMessage.msg(toWho,orderId,toId,msgType,message));
    }
}
