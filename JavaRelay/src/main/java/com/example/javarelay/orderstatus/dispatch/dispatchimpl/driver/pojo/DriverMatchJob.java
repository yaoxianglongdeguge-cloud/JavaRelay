package com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.pojo;

import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.metric.DriverMetric;
import lombok.Data;

//队列一个任务
@Data
public class DriverMatchJob {

    private Long orderId;

    private Long passengerId;

    private DriverMetric metric;

    // 订单起点，用来算与候选司机的距离
    private double startLng;

    private double startLat;

    private long enqueueAt;
}