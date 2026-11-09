package com.example.javarelay.Status;

public enum OrderOperatorEnum {
    PASSENGER(1, "乘客"),
    DRIVER(2, "司机"),
    SYSTEM(3, "系统");

    private final Integer code;
    private final String desc;

    OrderOperatorEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}