package com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.dipatcher;

import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.DriverMatchConstants;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.pojo.DriverMatchJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class DriverMatchQueue {

    private final BlockingQueue<DriverMatchJob> queue =
            new ArrayBlockingQueue<>(DriverMatchConstants.CAPACITY);

    private final Set<Long> queued = ConcurrentHashMap.newKeySet();

    // false = 队列已满；同一订单重复入队视为成功
    public boolean offer(DriverMatchJob job) {

        if (job == null || job.getOrderId() == null) {
            return false;
        }
        if (!queued.add(job.getOrderId())) {
            return true;
        }
        if (!queue.offer(job)) {
            queued.remove(job.getOrderId());
            return false;
        }
        return true;
    }

    public List<DriverMatchJob> drain(int max) {
        List<DriverMatchJob> batch = new ArrayList<>(max);
        queue.drainTo(batch, max);
        for (DriverMatchJob job : batch) {
            queued.remove(job.getOrderId());
        }
        return batch;
    }

    public boolean requeue(DriverMatchJob job) {

        if (job == null || job.getOrderId() == null) {
            return false;
        }
        if (!queued.add(job.getOrderId())) {
            return true;
        }
        if (!queue.offer(job)) {
            queued.remove(job.getOrderId());
            log.error("订单 {} 放回派司机队列失败（队列已满）", job.getOrderId());
            return false;
        }
        return true;
    }

    public int size() {
        return queue.size();
    }

    public boolean contains(Long orderId) {
        return orderId != null && queued.contains(orderId);
    }
}