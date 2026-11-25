package com.example.javarelay.orderstatus.dispatch;

import com.example.javarelay.orderstatus.dispatch.DriverCandidateProvider.OrderCandidates;
import com.example.javarelay.pojo.DispatchMetric;
import com.example.javarelay.pojo.DriverCandidate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 收益矩阵构造者：把"订单 × 候选司机"变成匈牙利算法要的 profit / allowed 矩阵。
 *
 * <p>指标（权重）在这里生效：{@code profit = metric.profit(距离, 评分)}。
 */
@Slf4j
@Component
public class DispatchMatrixBuilder {

    public DispatchMatrix build(List<DispatchJob> jobs, List<OrderCandidates> candidatesByOrder) {

        // 1) 候选并集，只保留空闲司机
        Set<Long> idleIds = new LinkedHashSet<>();
        Map<Long, DriverCandidate> driverInfo = new LinkedHashMap<>();
        int candidateCount = 0;

        for (OrderCandidates candidates : candidatesByOrder) {

            if (candidates == null) {
                continue;
            }

            candidateCount += candidates.size();

            for (DriverCandidate candidate : candidates.all()) {
                if (candidate.isIdle()) {
                    idleIds.add(candidate.getId());
                    driverInfo.putIfAbsent(candidate.getId(), candidate);
                }
            }
        }

        List<Long> driverIds = new ArrayList<>(idleIds);

        if (driverIds.size() > DispatchConstants.MAX_DRIVERS) {
            driverIds = new ArrayList<>(driverIds.subList(0, DispatchConstants.MAX_DRIVERS));
        }

        // 2) 填矩阵
        int rows = jobs.size();
        int cols = driverIds.size();

        long[][] profit = new long[rows][cols];
        boolean[][] allowed = new boolean[rows][cols];

        int noDistance = 0;
        int attrMissing = 0;

        for (int i = 0; i < rows; i++) {

            DispatchJob job = jobs.get(i);
            DispatchMetric metric = job.getMetric();
            OrderCandidates candidates = candidatesByOrder.get(i);

            for (int j = 0; j < cols; j++) {

                Long driverId = driverIds.get(j);
                DriverCandidate candidate = candidates == null ? null : candidates.get(driverId);

                // 该订单半径内没有这个司机（或被并集带进来但对他超距）→ 不可派
                if (candidate == null) {
                    noDistance++;
                    continue;
                }

                if (candidate.isAttrMissing()) {
                    attrMissing++;
                }

                profit[i][j] = Math.round(metric.profit(
                        candidate.getDistanceMeters(), candidate.getScore()));
                allowed[i][j] = true;
            }
        }

        // 属性缺失按"司机"去重统计，不是按组合
        int attrMissingDrivers = 0;
        for (DriverCandidate candidate : driverInfo.values()) {
            if (candidate.isAttrMissing()) {
                attrMissingDrivers++;
            }
        }

        return new DispatchMatrix(driverIds, profit, allowed, candidateCount, noDistance, attrMissingDrivers);
    }
}