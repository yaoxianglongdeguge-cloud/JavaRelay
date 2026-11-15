package com.example.javarelay.messagetask;

import com.example.javarelay.constant.Constants;
import com.example.javarelay.strategys.*;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.message.Message;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class MessageDispatcher {

    @Autowired
    List<MessageHandler> handlerList;
    Map<String,MessageHandler> handlers = new HashMap<>();

    private final BlockingQueue<MessageCell> bufferQueue = new ArrayBlockingQueue<>(Constants.MESSAGE_TASK_CAPACITY);

    @PostConstruct
    public void init(){
        for(MessageHandler s:handlerList){
            handlers.put(s.getTopic(),s);
        }

        new Thread(() -> {
            while (true) {
                try {
                    MessageCell msg = bufferQueue.poll(Constants.MESSAGE_INTERVAL_TIME, TimeUnit.SECONDS);
                    if (msg != null) {
                        MessageHandler handler = handlers.get(msg.getTopic());
                        if (handler != null) {
                            handler.handler(msg);
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    public boolean putMessage(MessageCell messageCell) {
        if (messageCell == null) {
            return false;
        }
        return bufferQueue.offer(messageCell);
    }
}
