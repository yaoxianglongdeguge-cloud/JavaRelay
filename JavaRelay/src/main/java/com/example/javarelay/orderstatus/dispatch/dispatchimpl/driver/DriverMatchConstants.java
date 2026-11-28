package com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver;

public final class DriverMatchConstants {

    private DriverMatchConstants() {
    }

    public static final int CAPACITY = 5000;

    public static final int BATCH_SIZE = 20;

    public static final long INTERVAL_MS = 500L;

    // 在队列里超过这个时间还没匹配上，判定为没匹配到
    public static final long MATCH_DEADLINE_MS = 60_000L;

    // 候选司机查询半径（米）
    public static final double CANDIDATE_RADIUS_METERS = 5000;

    // 单个订单最多取多少个附近司机
    public static final int CANDIDATE_LIMIT = 200;
}