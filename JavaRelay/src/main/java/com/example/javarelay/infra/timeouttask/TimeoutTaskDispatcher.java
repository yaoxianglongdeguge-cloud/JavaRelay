package com.example.javarelay.infra.timeouttask;

import com.example.javarelay.config.RootMessageTaskConfig;
import com.example.javarelay.data.entity.Order;
import com.example.javarelay.statustransition.exception.StatusChangeException;
import com.example.javarelay.statustransition.statusenum.DriverStatusEnum;
import com.example.javarelay.statustransition.statusenum.OrderOperatorEnum;
import com.example.javarelay.statustransition.statusenum.OrderStatusEnum;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Component
public class TimeoutTaskDispatcher {
    @Autowired
    List<DelayMessageHandler> handlerList;
    Map<String,DelayMessageHandler> handlers = new HashMap<>();

    private final DelayQueue<DelayMessageCell> delayQueue = new DelayQueue<>();

    @PostConstruct
    public void init(){
        for(DelayMessageHandler s:handlerList){
            handlers.put(s.getTopic(),s);
        }

        new Thread(() -> {
            while (true) {
                try {
                    DelayMessageCell msg = delayQueue.poll(RootMessageTaskConfig.MESSAGE_INTERVAL_TIME, TimeUnit.SECONDS);
                    if (msg != null) {
                        DelayMessageHandler handler = handlers.get(msg.getTopic());
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

    public void addTimeoutTask(DelayMessageCell delayMessageCell) {
        delayQueue.put(delayMessageCell);
    }

}
