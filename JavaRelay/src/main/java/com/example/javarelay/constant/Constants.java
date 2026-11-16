package com.example.javarelay.constant;

public class Constants {

    public static final String PASSENGER_SELECT_TYPE = "default";
    public static final String PATH_SELECT_TYPE = "default";
    public static final String PRECOMPUTE_MONEY_TYPE = "default";

    public static final Integer ACTIVE_ORDER = 1;
    public static final Integer FINISHED_ORDER = null;

    public static final Integer MESSAGE_TASK_CAPACITY = 10000;
    public static final Integer MESSAGE_INTERVAL_TIME = 1;

    public static final String MESSAGE_COMPUTEMONEY_TOPIC = "computemoney";
    public static final String MESSAGE_INFORM_TOPIC = "inform";

    public static final Integer TO_DRIVER = 1;
    public static final Integer TO_PASSENGER = 2;

    public static final Integer ORDER_TIMEOUT_TIME = 15;

}
