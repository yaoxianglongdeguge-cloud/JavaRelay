package com.example.javarelay.orderstatus.backgroundtask;

import com.example.javarelay.orderstatus.constants.Constants;
import com.example.javarelay.orderstatus.constants.statusenum.OrderStatusEnum;
import com.example.javarelay.orderstatus.dispatch.DispatchConstants;
import com.example.javarelay.orderstatus.dispatch.DispatchExecutor;
import com.example.javarelay.orderstatus.dispatch.DispatchJob;
import com.example.javarelay.orderstatus.dispatch.DispatchMatrix;
import com.example.javarelay.orderstatus.dispatch.DispatchMatrixBuilder;
import com.example.javarelay.orderstatus.dispatch.DispatchQueue;
import com.example.javarelay.orderstatus.dispatch.DispatchSolver;
import com.example.javarelay.orderstatus.dispatch.DriverCandidateProvider;
import com.example.javarelay.orderstatus.dispatch.DriverCandidateProvider.OrderCandidates;
import com.example.javarelay.orderstatus.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 批量分配任务：只负责"取一批 → 匹配 → 落库 → 回队"。
 *
 * <p>队列在 {@link DispatchQueue}，失败决策在 {@code EnqueueRetryPolicy}，
 * 本类不再承担这两件事。
 */
@Slf4j
@Component
public class BatchDispatchTask {

    @Autowired
    DispatchQueue dispatchQueue;

    @Autowired
    OrderService orderService;

    @Autowired
    DriverCandidateProvider candidateProvider;

    @Autowired
    DispatchMatrixBuilder matrixBuilder;

    @Autowired
    DispatchSolver solver;

    @Autowired
    DispatchExecutor executor;

    public int size() {
        return dispatchQueue.size();
    }

    public boolean contains(Long orderId) {
        return dispatchQueue.contains(orderId);
    }

    @Scheduled(fixedDelay = DispatchConstants.INTERVAL_MS)
    public void match() {

        List<DispatchJob> batch = dispatchQueue.drain(DispatchConstants.BATCH_SIZE);

        if (batch.isEmpty()) {
            return;
        }

        List<DispatchJob> alive = dropExpired(batch);

        if (alive.isEmpty()) {
            return;
        }

        // 1) 候选
        List<OrderCandidates> candidates = new ArrayList<>(alive.size());
        for (DispatchJob job : alive) {
            candidates.add(candidateProvider.candidatesOf(job));
        }

        // 2) 矩阵
        DispatchMatrix matrix = matrixBuilder.build(alive, candidates);

        if (matrix.isEmpty()) {
            log.info("批次分配：{} 单，附近 5km 内没有可用司机，全部留在队列", alive.size());
            requeue(alive);
            return;
        }

        // 3) 求解
        DispatchSolver.Solution solution = solver.solve(matrix);

        // 4) 落库
        Set<Long> failed = executor.execute(alive, matrix, solution);

        List<DispatchJob> leftover = new ArrayList<>();
        for (DispatchJob job : alive) {
            if (failed.contains(job.getOrderId())) {
                leftover.add(job);
            }
        }

        log.info("批次分配[{}]：订单 {} 单 / 半径内候选 {} 个 / 可用 {} 个 -> 匹配 {} 组，派出 {}，失败 {}，"
                        + "剩余 {}，超半径组合 {}，属性缺失 {}，KM 耗时 {} us",
                metricNames(alive), alive.size(), matrix.getCandidateCount(), matrix.cols(),
                solution.matchedCount(), solution.matchedCount() - failed.size(), failed.size(),
                leftover.size(), matrix.getNoDistanceCount(), matrix.getAttrMissingCount(),
                solution.getMicros());

        if (!leftover.isEmpty()) {
            requeue(leftover);
        }
    }

    /** 掉已经超过截止时间还没派出去的订单（取消，不再参与匹配） */
    private List<DispatchJob> dropExpired(List<DispatchJob> batch) {

        long now = System.currentTimeMillis();
        List<DispatchJob> alive = new ArrayList<>(batch.size());

        for (DispatchJob job : batch) {

            if (now - job.getEnqueueAt() > Constants.REDISPATCH_DEADLINE) {
                log.warn("订单 {} 在分配队列中等待超时，取消", job.getOrderId());
                try {
                    orderService.cancelAfterDispatchFail(job.getOrderId(), job.getPassengerId(),
                            OrderStatusEnum.WAITING_DISPATCH.getCode(), null, "等待分配超时");
                } catch (Exception e) {
                    log.error("取消超时订单失败 orderId={}", job.getOrderId(), e);
                }
                continue;
            }

            alive.add(job);
        }

        return alive;
    }

    private void requeue(List<DispatchJob> jobs) {
        for (DispatchJob job : jobs) {
            dispatchQueue.requeue(job);
        }
    }

    private String metricNames(List<DispatchJob> jobs) {

        Set<String> names = new HashSet<>();

        for (DispatchJob job : jobs) {
            names.add(job.getMetric() == null ? "?" : job.getMetric().getName());
        }

        return String.join(",", names);
    }
}