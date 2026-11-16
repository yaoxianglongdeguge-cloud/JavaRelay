package com.example.javarelay.messagetask.MessageHandlersImpl;

import com.example.javarelay.constant.Constants;
import com.example.javarelay.constant.InformTypeEnum;
import com.example.javarelay.messagetask.MessageCell;
import com.example.javarelay.messagetask.MessageCells.InformMessage;
import com.example.javarelay.messagetask.MessageHandler;
import com.example.javarelay.ordertimeoutmanage.OrderTimeoutManager;
import com.example.javarelay.websocket.MyWebSocketHandler;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

public class InformHandler implements MessageHandler {

    @Autowired
    OrderTimeoutManager orderTimeoutManager;

    @Autowired
    MyWebSocketHandler webSocketHandler;

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
        String message = msg.getMessage();
        Long orderId = msg.getOrderId();

        if(type == InformTypeEnum.STATUS_CONFIRM.getCode()){

            //通知对方来确认状态
            informStatusConfirm(toWho,msg.getToId(),message);

            if(toWho.equals(Constants.TO_DRIVER) && message.equals("已派单")){
                //如果是通知司机来接单，那还要维护超时任务，防止司机长时间不接单
                orderTimeoutManager.addTimeoutTask(orderId);
            }

        }

        return true;
    }

    public void informStatusConfirm(Integer toWho,Long toId,String message){
        webSocketHandler.sendToUser(toWho,toId,message);
    }
}
