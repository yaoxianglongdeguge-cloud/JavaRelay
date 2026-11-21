package com.example.javarelay.userinform.mqtask.messagecells;

import com.example.javarelay.infra.mq.MessageCell;
import lombok.Data;

@Data
public class InformMessage extends MessageCell {

    @Data
    public class msg{

        Integer toWho;
        Long orderId;
        Long meId;
        Long toId;
        Integer msgType;
        String message;

        public msg(Integer toWho,Long orderId,Long meId,Long toId,Integer msgType,String message){

            this.toWho = toWho;
            this.msgType = msgType;
            this.meId = meId;
            this.message = message;
            this.orderId = orderId;
            this.toId = toId;
        }
    }

    public InformMessage(Integer toWho,Long orderId,Long meId,Long toId,Integer msgType,String message){
        this.setTopic(com.example.javarelay.userinform.config.MessageTaskConfig.MESSAGE_INFORM_TOPIC);
        this.setMsg(new InformMessage.msg(toWho,orderId,meId,toId,msgType,message));
    }
}
