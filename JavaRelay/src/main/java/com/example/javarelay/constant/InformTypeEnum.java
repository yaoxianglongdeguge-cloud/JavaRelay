package com.example.javarelay.constant;

import com.example.javarelay.status.DriverStatusEnum;

public enum InformTypeEnum {

    STATUS_CONFIRM(0, "状态确认");

    private final int code;
    private final String desc;


    InformTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }

    public static InformTypeEnum of(int code) {
        for (InformTypeEnum s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("未知状态码: " + code);
    }

}
