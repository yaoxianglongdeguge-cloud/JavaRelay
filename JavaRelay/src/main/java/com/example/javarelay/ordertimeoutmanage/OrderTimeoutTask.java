package com.example.javarelay.ordertimeoutmanage;

import lombok.Data;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Data
public class OrderTimeoutTask implements Delayed {

    private Long orderId;
    private long executeTime; // 任务到期的时间戳（毫秒）

    public OrderTimeoutTask(Long orderId, long delaySeconds) {
        this.orderId = orderId;
        this.executeTime = System.currentTimeMillis() + delaySeconds * 1000;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long diff = executeTime - System.currentTimeMillis();
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
        OrderTimeoutTask otherTask = (OrderTimeoutTask) other;
        return Long.compare(this.executeTime, otherTask.executeTime);
    }
}