package com.example.javarelay.messagetask.MessageHandlersImpl;

import com.example.javarelay.constant.Constants;
import com.example.javarelay.messagetask.MessageCell;
import com.example.javarelay.messagetask.MessageCells.InformMessage;
import com.example.javarelay.messagetask.MessageHandler;

public class InformHandler implements MessageHandler {

    public String topic = Constants.MESSAGE_INFORM_TOPIC;

    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public Boolean handler(MessageCell messageCell) {

        InformMessage informMessage = (InformMessage) messageCell;
        InformMessage.msg msg = (InformMessage.msg) informMessage.getMsg();
        Integer toWho = msg.getToWho();
        Integer type = msg.getMsgType();
        

        return null;
    }
}
