package com.example.javarelay.statustransition.messagetask.messagehandlersimpl;

import com.example.javarelay.infra.messagetask.MessageCell;
import com.example.javarelay.infra.messagetask.MessageHandler;
import com.example.javarelay.statustransition.config.MessageTaskConfig;
import com.example.javarelay.statustransition.constants.Constants;
import com.example.javarelay.statustransition.patroltask.DriverPatrolTask;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static jodd.util.ThreadUtil.sleep;

public class DriverPatrolHandlerImpl implements MessageHandler {

    @Autowired
    DriverPatrolTask driverPatrolTask;

    public String topic = MessageTaskConfig.DRIVER_PATROL_TOPIC;

    ThreadPoolExecutor Executor = new ThreadPoolExecutor(
            1, 4, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(200),
            r -> new Thread(r, "use-driver-patrol-worker"),
            new ThreadPoolExecutor.DiscardPolicy()
    );

    ThreadPoolExecutor driverPatrolExecutor = new ThreadPoolExecutor(
            1, 2, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(40),
            r -> new Thread(r, "driver-patrol-worker"),
            new ThreadPoolExecutor.DiscardPolicy()
    );

    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public Boolean handler(MessageCell messageCell) {

        Executor.execute(()->handle());

        return true;
    }

    private void handle(){

        long now = System.currentTimeMillis();
        long lastTs = driverPatrolTask.lastHeartbeat.get();
        long diffMs = now - lastTs;

        if(diffMs <= Constants.PATROL_HEART){
            return;
        }

        Future<?> future;
        if( (future = driverPatrolTask.getPatrolFuture()) != null){
            future.cancel(true);
        }

        driverPatrolTask.setPatrolFuture(
                driverPatrolExecutor.submit(driverPatrolTask)
        );


    }
}
