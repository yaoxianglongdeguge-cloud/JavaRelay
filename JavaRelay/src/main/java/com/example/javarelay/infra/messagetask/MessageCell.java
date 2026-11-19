package com.example.javarelay.infra.messagetask;

import com.example.javarelay.statustransition.messagetask.messagecells.OrderTimeoutMessage;
import jakarta.annotation.Nonnull;
import lombok.Data;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Data
public class MessageCell implements Delayed {

    private String topic;
    private long executeTime; // 任务到期的时间戳（毫秒）
    private Object msg;


    @Override
    public long getDelay(TimeUnit unit) {
        long diff = executeTime - System.currentTimeMillis();
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }
    @Override
    public final int compareTo(@Nonnull Delayed other) {
        OrderTimeoutMessage otherTask = (OrderTimeoutMessage) other;
        return Long.compare(this.executeTime, otherTask.getExecuteTime());
    }

}
