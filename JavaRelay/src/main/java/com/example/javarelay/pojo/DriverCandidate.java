package com.example.javarelay.pojo;

import com.example.javarelay.orderstatus.constants.statusenum.DriverStatusEnum;
import lombok.Data;

/**
 * 候选司机：一次匹配中"某个订单眼里的一个司机"。
 *
 * <p>三块数据的来源：
 * <ul>
 *   <li>{@link #distanceMeters} —— GEO 半径查询（订单相关，每次现算）</li>
 *   <li>{@link #status} —— {@code service:driver:info:{id}}（秒级缓存）</li>
 *   <li>{@link #score} —— {@code service:driver:attr:{id}}（分钟级缓存）</li>
 * </ul>
 *
 * <p>替代了原来的 {@code DriverGeo} 和 {@code BatchDispatchTask.DriverSnapshot}。
 */
@Data
public class DriverCandidate {

    private Long id;

    /** 到订单起点的直线距离（米） */
    private double distanceMeters;

    /** 司机状态；null 表示状态缓存缺失（视为不可用） */
    private Integer status;

    /** 评分；缓存缺失时取默认值 */
    private double score;

    /** 评分是否来自兜底默认值（用于观测告警） */
    private boolean attrMissing;

    public boolean isIdle() {
        return status != null && status.equals(DriverStatusEnum.LEISURE.getCode());
    }
}