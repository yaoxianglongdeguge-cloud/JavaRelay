package com.example.javarelay.statustransition.messagetask.messagehandlersimpl;

import com.example.javarelay.statustransition.config.MessageTaskConfig;
import com.example.javarelay.statustransition.constants.Constants;
import com.example.javarelay.infra.messagetask.MessageCell;
import com.example.javarelay.statustransition.messagetask.messagecells.ReDispatchMessage;
import com.example.javarelay.infra.messagetask.MessageHandler;
import com.example.javarelay.statustransition.service.PassengerOperateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
@Slf4j
@Component
public class ReDispatchHandlerImpl implements MessageHandler {
    String topic = MessageTaskConfig.MESSAGE_REDISPATCH_TOPIC;

    @Autowired
    PassengerOperateService passengerOperateService;

    ThreadPoolExecutor computeMoneyExecutor = new ThreadPoolExecutor(
            4, 8, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(2000),
            r -> new Thread(r, "reDisPatch-worker"),
            new ThreadPoolExecutor.AbortPolicy()
    );

    @Override
    public String getTopic() {
        return this.topic;
    }

    @Override
    public Boolean handler(MessageCell messageCell) {

        try{

            computeMoneyExecutor.execute(()->handle(messageCell));

        }catch(RejectedExecutionException e){
            log.info("重派单线程池已满");
        }

        return true;

    }

    public void handle(MessageCell messageCell){
        ReDispatchMessage reDispatchMessage = (ReDispatchMessage) messageCell;
        ReDispatchMessage.msg msg = (ReDispatchMessage.msg) reDispatchMessage.getMsg();
        Long orderId = msg.getOrderId();
        Long passengerId = msg.getPassengerId();

        passengerOperateService.ReDispatchOrder(orderId,passengerId);
    }
}
