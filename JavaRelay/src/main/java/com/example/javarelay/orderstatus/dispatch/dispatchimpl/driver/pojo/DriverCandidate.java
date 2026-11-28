package com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.pojo;

import lombok.Data;

@Data
public class DriverCandidate {

    private Long driverId;

    // 与订单起点的距离（米）
    private double distance;

    // 空闲时长（毫秒）
    private long idleMillis;
}