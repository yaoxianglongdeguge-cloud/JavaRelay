package com.example.javarelay.infra.messagetask;

import com.example.javarelay.config.RootMessageTaskConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;

@Slf4j
@Component
public class MessageDispatcher {

    @Autowired
    List<MessageHandler> handlerList;
    Map<String,MessageHandler> handlers = new HashMap<>();

    private final DelayQueue<MessageCell> delayQueue = new DelayQueue<>();
    private final Queue<MessageCell> unDelayQueue = new ArrayBlockingQueue<>(RootMessageTaskConfig.MESSAGE_TASK_CAPACITY);

    @PostConstruct
    public void init(){
        for(MessageHandler s:handlerList){
            handlers.put(s.getTopic(),s);
        }

        new Thread(() -> {
            while (true) {
                try {
                    MessageCell msg = delayQueue.poll(RootMessageTaskConfig.MESSAGE_INTERVAL_TIME, TimeUnit.SECONDS);
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

        new Thread(() -> {
            while (true) {
                try {
                    MessageCell msg = unDelayQueue.poll();
                    if (msg != null) {
                        MessageHandler handler = handlers.get(msg.getTopic());
                        if (handler != null) {
                            handler.handler(msg);
                        }
                    }
                } catch (Exception e) {
                    break;
                }
            }
        }).start();


    }

    public boolean addTask(MessageCell messageCell) {
        if(messageCell.getExecuteTime() == 0L){
            unDelayQueue.add(messageCell);
            return true;
        }
        delayQueue.put(messageCell);
        return true;
    }

}
