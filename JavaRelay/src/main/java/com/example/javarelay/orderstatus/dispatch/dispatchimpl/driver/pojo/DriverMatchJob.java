package com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.pojo;

import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.metric.DriverMetric;
import lombok.Data;

@Data
public class DriverMatchJob {

    private Long orderId;

    private Long passengerId;

    private DriverMetric metric;

    private long enqueueAt;
}