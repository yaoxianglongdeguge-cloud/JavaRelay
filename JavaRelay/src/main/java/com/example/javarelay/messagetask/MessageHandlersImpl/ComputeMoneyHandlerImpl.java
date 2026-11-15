package com.example.javarelay.messagetask.MessageHandlersImpl;

import com.example.javarelay.constant.Constants;
import com.example.javarelay.infocollect.InfoCollector;
import com.example.javarelay.messagetask.MessageCell;
import com.example.javarelay.messagetask.MessageCells.ComputeMoneyMessage;
import com.example.javarelay.messagetask.MessageHandler;

public class ComputeMoneyHandlerImpl implements MessageHandler {

    public String topic = Constants.MESSAGE_COMPUTEMONEY_TOPIC;

    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public Boolean handler(MessageCell messageCell) {

        ComputeMoneyMessage computeMoneyMessage = (ComputeMoneyMessage) messageCell;
        ComputeMoneyMessage.msg msg = (ComputeMoneyMessage.msg)computeMoneyMessage.getMsg();
        InfoCollector infoCollector = msg.getInfoCollector();



        return null;
    }
}
