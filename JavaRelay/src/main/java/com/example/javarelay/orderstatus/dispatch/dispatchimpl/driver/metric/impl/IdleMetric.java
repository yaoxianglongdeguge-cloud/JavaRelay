package com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.metric.impl;

import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.pojo.DriverCandidate;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.metric.DriverMetric;
import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class IdleMetric implements DriverMetric {

    // 空闲超过这个时长按满分算（毫秒）
    private long capMillis = 600_000L;

    private double weight = 1.0;

    @Override
    public String type() {
        return "idle";
    }

    @Override
    public double score(DriverCandidate candidate) {
        long idle = Math.min(candidate.getIdleMillis(), capMillis);
        return weight * ((double) idle / capMillis);
    }
}