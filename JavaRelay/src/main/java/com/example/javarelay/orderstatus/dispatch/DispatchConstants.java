package com.example.javarelay.orderstatus.dispatch;

/**
 * 批量分配骨架的常量。
 *
 * <p>当前只有"队列 + 定时任务"的骨架，匹配实现待补。
 */
public final class DispatchConstants {

    private DispatchConstants() {
    }

    /** 队列容量：满了策略返回失败 */
    public static final int CAPACITY = 5000;

    /** 一轮最多取多少个订单 */
    public static final int BATCH_SIZE = 20;

    /** 每轮间隔（毫秒） */
    public static final long INTERVAL_MS = 500L;
}