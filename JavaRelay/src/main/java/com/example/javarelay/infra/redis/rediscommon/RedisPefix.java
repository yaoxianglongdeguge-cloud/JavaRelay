package com.example.javarelay.infra.redis.rediscommon;

public class RedisPefix {

    public static final String SERVICEAREA_PREFIX = "service:area:info:";
    public static final String SERVICEAREA_LOCK_PREFIX = "lock:area:info:";
    public static final String DRIVER_INFO = "service:driver:info:";
    public static final String STUCK_ORDER_LOCK_KEY = "lock:task:stuck-order";
    public static final long STUCK_ORDERLEADER_TTL = 40L;
    public static final String DRIVER_PATROL_LOCK_KEY = "lock:task:driver-patrol";
    public static final long DRIVER_PATROL_LEADER_TTL = 2L;
    public static final String WEBSOCKET_CHANNEL = "ws:push";
}
