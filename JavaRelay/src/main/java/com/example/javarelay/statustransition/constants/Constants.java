package com.example.javarelay.statustransition.constants;

public class Constants {

    public static final long DISPATCH_TIMEOUT_MS = 1000;//如果匹配到司机但是没抢到锁，此时会进行重试，但是要有时间上界
    public static final long RETRY_INTERVAL_MS  = 300;
    public static final long REDISPATCH_DEADLINE = 60000;


    public static final Integer ACTIVE_ORDER = 1;
    public static final Integer FINISHED_ORDER = null;


    public static final Integer ORDER_TIMEOUT_TIME = 15;

    public static final Integer DRIVER_RECEIVE = 1;
    public static final Integer DRIVER_DISRECEIVE = 0;

    public static final int SERVE_AREA_SCAN = 5000;
    public static final int SERVE_DRIVERS_SCAN = 5000;

    public static final int DRIVER_PATROL_INTERVAL = 3000;
}
