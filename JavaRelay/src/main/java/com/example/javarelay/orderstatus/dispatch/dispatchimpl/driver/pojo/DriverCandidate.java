package com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.pojo;

import lombok.Data;

@Data
public class DriverCandidate {

    private Long driverId;

    // 司机当前坐标
    private double lng;

    private double lat;

    // 与订单起点的距离（米），由引擎按订单逐个填充
    private double distance;

    // 空闲时长（毫秒）
    private long idleMillis;

    // 评分（driver_detail.score）
    private double score;
}