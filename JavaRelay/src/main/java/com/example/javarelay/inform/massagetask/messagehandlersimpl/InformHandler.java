package com.example.javarelay.inform.massagetask.messagehandlersimpl;

import com.example.javarelay.config.RootInformConfig;
import com.example.javarelay.infra.mq.MessageCell;
import com.example.javarelay.inform.constants.InformTypeEnum;
import com.example.javarelay.inform.massagetask.messagecells.InformMessage;
import com.example.javarelay.infra.mq.MessageHandler;
import com.example.javarelay.infra.websocket.MyWebSocketHandler;
import com.example.javarelay.inform.config.MessageTaskConfig;
import com.example.javarelay.statustransition.constants.Constants;
import com.example.javarelay.statustransition.messagetask.messagecells.OrderTimeoutMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class InformHandler implements MessageHandler {


    @Autowired
    MyWebSocketHandler webSocketHandler;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public String topic = MessageTaskConfig.MESSAGE_INFORM_TOPIC;

    ThreadPoolExecutor Executor = new ThreadPoolExecutor(
            1, 4, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(200),
            r -> new Thread(r, "inform-worker"),
            new ThreadPoolExecutor.DiscardPolicy()
    );

    @Override
    public String getTopic() {
        return topic;
    }


    @Override
    public Boolean handler(MessageCell messageCell) {

        Executor.execute(()->handle(messageCell));

        return true;
    }

    public void handle(MessageCell messageCell){

        InformMessage informMessage = (InformMessage) messageCell;
        InformMessage.msg msg = (InformMessage.msg) informMessage.getMsg();
        Integer toWho = msg.getToWho();
        Integer type = msg.getMsgType();
        String message = msg.getMessage();
        Long orderId = msg.getOrderId();

        if(type == InformTypeEnum.STATUS_CONFIRM.getCode()){

            //通知对方来确认状态
            informStatusConfirm(toWho,msg.getToId(),message);

            if(toWho.equals(RootInformConfig.TO_DRIVER) && message.equals("已派单")){
                //如果是通知司机来接单，那还要维护超时任务，防止司机长时间不接单
                OrderTimeoutMessage messageCell1 = new OrderTimeoutMessage(orderId,Constants.ORDER_TIMEOUT_TIME);

                eventPublisher.publishEvent(messageCell1);
            }

        }
    }

    public void informStatusConfirm(Integer toWho,Long toId,String message){
        webSocketHandler.sendToUser(toWho,toId,message);
    }
}
