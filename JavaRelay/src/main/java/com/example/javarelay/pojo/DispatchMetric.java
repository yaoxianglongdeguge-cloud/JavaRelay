package com.example.javarelay.pojo;

import lombok.Data;

@Data
public class DispatchMetric {

    /** 策略名，写日志便于对比效果 */
    private String name;

    /** 距离权重：越大越偏好近的司机 */
    private double distanceWeight = 1.0D;

    /** 评分权重：越大越偏好高评分司机 */
    private double scoreWeight = 0.0D;

    /** 评分换算成等效米数的系数 */
    private double scoreToMeter = 200.0D;

    public static DispatchMetric of(String name, double distanceWeight, double scoreWeight) {
        DispatchMetric metric = new DispatchMetric();
        metric.setName(name);
        metric.setDistanceWeight(distanceWeight);
        metric.setScoreWeight(scoreWeight);
        return metric;
    }

    public double profit(double distanceMeters, double score) {
        return -distanceWeight * distanceMeters + scoreWeight * score * scoreToMeter;
    }
}