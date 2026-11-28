package com.example.javarelay.orderstatus.config;

public class MQConfig {

    public static final String MESSAGE_COMPUTEMONEY_TOPIC = "computemoney";
    /** 首次派单入口 */
    public static final String MESSAGE_DISPATCH_TOPIC = "dispatch";

    /** 派单失败后的重试 */
    public static final String MESSAGE_REDISPATCH_TOPIC = "redispatch";


    // 派司机引擎
    public static final String MESSAGE_MATCH_DRIVER_TOPIC = "match_driver";

    public static final String ORDER_TIMEOUT_TOPIC = "delay_receive";


}
