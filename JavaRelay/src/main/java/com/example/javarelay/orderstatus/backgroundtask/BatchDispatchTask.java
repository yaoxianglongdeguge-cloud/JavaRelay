package com.example.javarelay.orderstatus.backgroundtask;

import com.example.javarelay.orderstatus.constants.Constants;
import com.example.javarelay.orderstatus.constants.statusenum.OrderOperatorEnum;
import com.example.javarelay.orderstatus.constants.statusenum.OrderStatusEnum;
import com.example.javarelay.orderstatus.dispatch.DispatchConstants;
import com.example.javarelay.orderstatus.dispatch.DispatchJob;
import com.example.javarelay.orderstatus.dispatch.DispatchQueue;
import com.example.javarelay.orderstatus.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量分配任务（骨架）。
 *
 * <p>现在只做：取一批 → 丢掉超时的 → doMatch（待实现）→ 没派成的放回队列。
 *
 * <p>接入匹配时，在 {@link #doMatch(List)} 里补：
 * <ol>
 *   <li>按订单坐标取候选司机</li>
 *   <li>构造收益矩阵</li>
 *   <li>求最优分配（最大权匹配）</li>
 *   <li>逐个落库，返回没派成的订单</li>
 * </ol>
 */
@Slf4j
@Component
public class BatchDispatchTask {

    @Autowired
    DispatchQueue dispatchQueue;

    @Autowired
    OrderService orderService;

    public int size() {
        return dispatchQueue.size();
    }

    public boolean contains(Long orderId) {
        return dispatchQueue.contains(orderId);
    }

    @Scheduled(fixedDelay = DispatchConstants.INTERVAL_MS)
    public void match() {

        List<DispatchJob> batch = dispatchQueue.drain(DispatchConstants.BATCH_SIZE);

        if (batch.isEmpty()) {
            return;
        }

        List<DispatchJob> alive = dropExpired(batch);

        if (alive.isEmpty()) {
            return;
        }

        log.info("批次分配：本轮取出 {} 单，有效 {} 单，队列剩余 {}",
                batch.size(), alive.size(), dispatchQueue.size());

        List<DispatchJob> leftover = doMatch(alive);

        if (leftover != null && !leftover.isEmpty()) {
            for (DispatchJob job : leftover) {
                dispatchQueue.requeue(job);
            }
        }
    }

    /**
     * 匹配逻辑的扩展点。
     *
     * <p>当前为空实现：所有订单原样退回队列，等下一轮。
     *
     * @return 本轮没有派单成功的订单（由调用方放回队列）
     */
    protected List<DispatchJob> doMatch(List<DispatchJob> batch) {
        // TODO 批量匹配
        return batch;
    }

    /** 掉已经超过截止时间还没派出去的订单（取消，不再参与匹配） */
    private List<DispatchJob> dropExpired(List<DispatchJob> batch) {

        long now = System.currentTimeMillis();
        List<DispatchJob> alive = new ArrayList<>(batch.size());

        for (DispatchJob job : batch) {

            if (now - job.getEnqueueAt() > Constants.REDISPATCH_DEADLINE) {
                log.warn("订单 {} 在分配队列中等待超时，取消", job.getOrderId());
                try {
                    orderService.cancelOrder(job.getOrderId(), job.getPassengerId(),
                            OrderStatusEnum.WAITING_DISPATCH.getCode(), null,
                            OrderOperatorEnum.SYSTEM.getCode(), "等待分配超时");
                } catch (Exception e) {
                    log.error("取消超时订单失败 orderId={}", job.getOrderId(), e);
                }
                continue;
            }

            alive.add(job);
        }

        return alive;
    }
}
