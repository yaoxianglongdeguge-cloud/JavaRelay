package com.example.javarelay.messagetask.MessageHandlersImpl;

import com.example.javarelay.constant.Constants;
import com.example.javarelay.messagetask.MessageCell;
import com.example.javarelay.messagetask.MessageCells.InformMessage;
import com.example.javarelay.messagetask.MessageCells.ReDispatchMessage;
import com.example.javarelay.messagetask.MessageHandler;
import com.example.javarelay.service.PassengerOrderService;
import org.springframework.beans.factory.annotation.Autowired;

public class ReDispatchHandlerImpl implements MessageHandler {
    String topic = Constants.MESSAGE_REDISPATCH_TOPIC;

    @Autowired
    PassengerOrderService passengerOrderService;
    @Override
    public String getTopic() {
        return this.topic;
    }

    @Override
    public Boolean handler(MessageCell messageCell) {

        ReDispatchMessage reDispatchMessage = (ReDispatchMessage) messageCell;
        ReDispatchMessage.msg msg = (ReDispatchMessage.msg) reDispatchMessage.getMsg();
        Long orderId = msg.getOrderId();
        Long passengerId = msg.getPassengerId();

        passengerOrderService.ReDispatchOrder(orderId,passengerId);

        return true;

    }
}
