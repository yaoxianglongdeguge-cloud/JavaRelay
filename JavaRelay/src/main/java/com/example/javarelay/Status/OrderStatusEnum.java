package com.example.javarelay.Status;

public enum OrderStatusEnum {

    WAITING_BIRTH(-1,"未生成"),
    WAITING_DISPATCH(0, "待派单"),
    DISPATCHED(1, "已派单"),
    RECEIVED(2, "已接单"),
    ONROAD(3,"行程中"),
    ARRIVED(4,"已到达"),
    WAITING_CONFIRM(5,"待确认"),
    CANCELED(6, "已取消"),
    FINISHED(7, "已完成");

    private final int code;
    private final String desc;


    OrderStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }

    public static OrderStatusEnum of(int code) {
        for (OrderStatusEnum s : values()) {
                if (s.code == code) return s;
        }
        throw new IllegalArgumentException("未知状态码: " + code);
    }

}
