package com.example.javarelay.pojo;

import lombok.Data;

/**
 * 附近司机（GEO 查询结果）：只有 id 和距离。
 *
 * <p>状态、评分等属性按需另取，不塞进这个对象。
 */
@Data
public class DriverGeo {

    private Long driverId;

    /** 到查询点的直线距离（米） */
    private double distanceMeters;
}