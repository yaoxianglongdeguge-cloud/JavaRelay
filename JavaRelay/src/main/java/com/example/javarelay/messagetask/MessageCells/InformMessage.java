package com.example.javarelay.messagetask.MessageCells;

import com.example.javarelay.infocollect.InfoCollector;
import com.example.javarelay.messagetask.MessageCell;
import lombok.Data;

public class InformMessage extends MessageCell {

    @Data
    public class msg{

        Integer toWho;
        String message;

        public msg(Integer toWho,String message){

            this.toWho = toWho;
            this.message = message;
        }
    }

    public InformMessage(Integer toWho,String message){
        this.setMsg(new InformMessage.msg(toWho,message));
    }
}
