package com.example.javarelay.orderstatus.dispatch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 待分配队列：只管"收不收"，不管"失败怎么办"。
 *
 * <p>刻意不依赖任何业务服务（尤其是 DispatchService），
 * 这样选车策略可以直接调用 {@link #offer} 拿到真实结果，
 * 而不会形成 {@code 策略 -> 队列 -> 服务 -> 策略} 的环。
 *
 * <p>失败处理（重试 / 取消补偿）由调用方决定。
 */
@Slf4j
@Component
public class DispatchQueue {

    private final BlockingQueue<DispatchJob> queue =
            new ArrayBlockingQueue<>(DispatchConstants.CAPACITY);

    /** 已在队列中的订单号，用于去重 */
    private final Set<Long> queued = ConcurrentHashMap.newKeySet();

    /**
     * 入队。
     *
     * @return false 表示队列已满（同一订单重复入队视为成功）
     */
    public boolean offer(DispatchJob job) {

        if (job == null || job.getOrderId() == null) {
            return false;
        }

        // 同一订单重复入队视为成功
        if (!queued.add(job.getOrderId())) {
            return true;
        }

        if (!queue.offer(job)) {
            queued.remove(job.getOrderId());
            return false;
        }

        return true;
    }

    /** 取一批；同时把这些订单从"在队"集合里移除 */
    public List<DispatchJob> drain(int max) {

        List<DispatchJob> batch = new ArrayList<>(max);
        queue.drainTo(batch, max);

        for (DispatchJob job : batch) {
            queued.remove(job.getOrderId());
        }

        return batch;
    }

    /** 放回队尾（取出来又没派成的） */
    public boolean requeue(DispatchJob job) {

        if (job == null || job.getOrderId() == null) {
            return false;
        }

        if (!queued.add(job.getOrderId())) {
            return true;
        }

        if (!queue.offer(job)) {
            queued.remove(job.getOrderId());
            log.error("订单 {} 放回分配队列失败（队列已满）", job.getOrderId());
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