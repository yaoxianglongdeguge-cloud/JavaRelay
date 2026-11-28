package com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.metric.impl;

import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.metric.DriverMetric;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.pojo.DriverCandidate;
import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class ScoreMetric implements DriverMetric {

    // 低于这个分不派
    private double minScore = 3.0;

    // 达到这个分算满分
    private double maxScore = 5.0;

    private double weight = 1.0;

    @Override
    public String type() {
        return "score";
    }

    @Override
    public double score(DriverCandidate candidate) {
        if (candidate.getScore() < minScore) {
            return 0;
        }
        if (candidate.getScore() >= maxScore) {
            return weight;
        }
        return weight * (candidate.getScore() - minScore) / (maxScore - minScore);
    }
}