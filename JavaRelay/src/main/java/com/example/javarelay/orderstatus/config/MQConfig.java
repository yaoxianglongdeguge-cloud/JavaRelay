package com.example.javarelay.orderstatus.config;

public class MQConfig {

    public static final String MESSAGE_COMPUTEMONEY_TOPIC = "computemoney";
    /** 首次派单入口 */
    public static final String MESSAGE_DISPATCH_TOPIC = "dispatch";

    /** 派单失败后的重试 */
    public static final String MESSAGE_REDISPATCH_TOPIC = "redispatch";
    public static final String MESSAGE_ASSIGNPASSENGER_TOPIC = "assignPassenger";
    public static final String MESSAGE_ASSIGNDRIVER_TOPIC = "assignDriver";
    public static final String MESSAGE_ASSIGNPATH_TOPIC = "assignPath";


    public static final String ORDER_TIMEOUT_TOPIC = "delay_receive";


}
