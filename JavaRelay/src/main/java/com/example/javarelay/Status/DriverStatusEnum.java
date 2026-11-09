package com.example.javarelay.status;

public enum DriverStatusEnum {

    LEISURE(0, "待派单"),
    RECEIVING(2, "已接单");

    private final int code;
    private final String desc;


    DriverStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }

    public static DriverStatusEnum of(int code) {
        for (DriverStatusEnum s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("未知状态码: " + code);
    }
}
