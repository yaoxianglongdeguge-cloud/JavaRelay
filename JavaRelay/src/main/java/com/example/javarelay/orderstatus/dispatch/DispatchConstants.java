package com.example.javarelay.orderstatus.dispatch;

/**
 * 批量分配相关常量。
 */
public final class DispatchConstants {

    private DispatchConstants() {
    }

    /** 队列容量：满了转 ReDispatch 重试 */
    public static final int CAPACITY = 5000;

    /** 一轮最多取多少个订单 */
    public static final int BATCH_SIZE = 20;

    /** 一轮最多考虑多少个候选司机 */
    public static final int MAX_DRIVERS = 200;

    /** 每轮间隔（毫秒） */
    public static final long INTERVAL_MS = 500L;

    /** 附近司机查询半径（米） */
    public static final double RADIUS_METERS = 5000D;

    /** 每个订单最多取多少个附近司机 */
    public static final int NEARBY_LIMIT = 200;

    /** 属性缓存缺失时的默认评分（与 driver_detail.score 默认值一致） */
    public static final double DEFAULT_SCORE = 5.0D;

    /** 入队失败最多重试几次 */
    public static final int MAX_ENQUEUE_RETRY = 3;

    /** 入队失败的重试间隔（毫秒） */
    public static final long RETRY_DELAY_MS = 500L;
}