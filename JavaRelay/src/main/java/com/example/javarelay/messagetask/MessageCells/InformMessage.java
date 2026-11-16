package com.example.javarelay.messagetask.MessageCells;

import com.example.javarelay.infocollect.InfoCollector;
import com.example.javarelay.messagetask.MessageCell;
import lombok.Data;

@Data
public class InformMessage extends MessageCell {

    @Data
    public class msg{

        Integer toWho;
        Integer msgType;
        String message;

        public msg(Integer toWho,Integer msgType,String message){

            this.toWho = toWho;
            this.msgType = msgType;
            this.message = message;
        }
    }

    public InformMessage(Integer toWho,Integer msgType,String message){
        this.setMsg(new InformMessage.msg(toWho,msgType,message));
    }
}
