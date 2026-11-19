package com.example.javarelay.inform.massagetask.messagehandlersimpl;

import com.example.javarelay.config.RootInformConfig;
import com.example.javarelay.infra.messagetask.MessageCell;
import com.example.javarelay.inform.constants.InformTypeEnum;
import com.example.javarelay.inform.massagetask.messagecells.InformMessage;
import com.example.javarelay.infra.messagetask.MessageHandler;
import com.example.javarelay.infra.messagetask.MessageDispatcher;
import com.example.javarelay.infra.websocket.MyWebSocketHandler;
import com.example.javarelay.inform.config.MessageTaskConfig;
import com.example.javarelay.statustransition.constants.Constants;
import com.example.javarelay.statustransition.messagetask.messagecells.ReDispatchMessage;
import org.springframework.beans.factory.annotation.Autowired;

public class InformHandler implements MessageHandler {

    @Autowired
    MessageDispatcher messageDispatcher;

    @Autowired
    MyWebSocketHandler webSocketHandler;

    public String topic = com.example.javarelay.inform.config.MessageTaskConfig.MESSAGE_INFORM_TOPIC;

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
        Long meId = msg.getMeId();

        if(type == InformTypeEnum.STATUS_CONFIRM.getCode()){

            //通知对方来确认状态
            informStatusConfirm(toWho,msg.getToId(),message);

            if(toWho.equals(RootInformConfig.TO_DRIVER) && message.equals("已派单")){
                //如果是通知司机来接单，那还要维护超时任务，防止司机长时间不接单
                ReDispatchMessage messageCell1 = new ReDispatchMessage(orderId,meId);
                messageCell1.setExecuteTime(Constants.ORDER_TIMEOUT_TIME);
                messageCell1.setTopic(com.example.javarelay.statustransition.
                        config.MessageTaskConfig.MESSAGE_REDISPATCH_TOPIC);

                messageDispatcher.addTask(messageCell1);
            }

        }

        return true;
    }

    public void informStatusConfirm(Integer toWho,Long toId,String message){
        webSocketHandler.sendToUser(toWho,toId,message);
    }
}
