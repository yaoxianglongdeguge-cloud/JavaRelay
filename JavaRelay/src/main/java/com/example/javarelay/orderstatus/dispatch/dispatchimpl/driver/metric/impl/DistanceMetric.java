package com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.metric.impl;

import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.pojo.DriverCandidate;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.metric.DriverMetric;
import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class DistanceMetric implements DriverMetric {

    // 超过这个距离不参与派单（米）
    private double maxDistance = 5000;

    private double weight = 1.0;

    @Override
    public String type() {
        return "default";
    }

    @Override
    public double score(DriverCandidate candidate) {
        if (candidate.getDistance() > maxDistance) {
            return 0;
        }
        return weight * (1 - candidate.getDistance() / maxDistance);
    }
}