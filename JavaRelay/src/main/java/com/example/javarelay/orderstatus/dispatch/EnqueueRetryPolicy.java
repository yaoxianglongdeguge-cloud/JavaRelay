package com.example.javarelay.orderstatus.dispatch;

import com.example.javarelay.data.entity.Order;
import com.example.javarelay.data.mapper.OrderMapper;
import com.example.javarelay.orderstatus.constants.Constants;
import com.example.javarelay.orderstatus.constants.statusenum.OrderStatusEnum;
import com.example.javarelay.orderstatus.mqtask.messagecells.ReDispatchMessage;
import com.example.javarelay.orderstatus.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 入队失败策略：全系统唯一"失败怎么办"的决策点。
 *
 * <p>语义：策略返回非 200 只有一个含义——<b>没入队</b>。
 * 处理只有两条路：
 * <ul>
 *   <li>还没到上限且没超时 → 延迟重试</li>
 *   <li>重试用尽或订单已超时 → 失败 + 取消补偿</li>
 * </ul>
 *
 * <p>注意：「入队成功但没车」不经过这里，它由队列的 60 秒超时兜底。
 */
@Slf4j
@Component
public class EnqueueRetryPolicy {

    @Autowired
    OrderMapper orderMapper;

    @Autowired
    OrderService orderService;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    /**
     * 处理一次入队失败。
     *
     * @param usedRetry 已经重试过的次数（首次失败传 0）
     * @param reason    失败原因，仅用于日志
     */
    public void onEnqueueFailed(Long orderId, Long passengerId, int usedRetry, String reason) {

        long ageMs = orderAgeMs(orderId);

        boolean retryExhausted = usedRetry >= DispatchConstants.MAX_ENQUEUE_RETRY;
        boolean timeout = ageMs > Constants.REDISPATCH_DEADLINE;

        if (retryExhausted || timeout) {

            log.error("订单 {} 提交分配失败（重试 {} 次，订单已存在 {} ms）：{}，取消并补偿",
                    orderId, usedRetry, ageMs, reason);

            orderService.cancelAfterDispatchFail(
                    orderId, passengerId,
                    OrderStatusEnum.WAITING_DISPATCH.getCode(),
                    null,
                    retryExhausted ? "分配队列持续不可用" : "提交分配超时");
            return;
        }

        log.warn("订单 {} 入队失败（第 {} 次重试）：{}", orderId, usedRetry + 1, reason);

        ReDispatchMessage retry = new ReDispatchMessage(orderId, passengerId);
        retry.getMsg().setRetryCount(usedRetry + 1);
        retry.setExecuteTime(System.currentTimeMillis() + DispatchConstants.RETRY_DELAY_MS);
        eventPublisher.publishEvent(retry);
    }

    private long orderAgeMs(Long orderId) {

        try {
            Order order = orderMapper.selectById(orderId);
            if (order == null || order.getCreateTime() == null) {
                return 0L;
            }
            return System.currentTimeMillis() - order.getCreateTime()
                    .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            log.error("查询订单创建时间失败 orderId={}", orderId, e);
            return 0L;
        }
    }
}