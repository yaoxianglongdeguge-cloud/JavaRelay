package com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.dipatcher;

import com.example.javarelay.infra.GeoUtil;
import com.example.javarelay.orderstatus.dispatch.dto.DispatchData;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.dipatcher.matcher.DriverMatcher;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.DriverMatchConstants;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.pojo.DriverCandidate;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.pojo.DriverMatchJob;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.pojo.DriverMatchResult;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.mqtask.messagecells.DriverMatchTimeoutMessage;
import com.example.javarelay.orderstatus.dispatch.mqtask.messagecells.DispatchMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class DriverDispatcher {

    @Autowired
    DriverMatchQueue driverMatchQueue;

    @Autowired
    DriverCandidateFinder driverCandidateFinder;

    @Autowired
    DriverMatcher driverMatcher;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedDelay = DriverMatchConstants.INTERVAL_MS)
    public void run() {

        List<DriverMatchJob> batch = driverMatchQueue.drain(DriverMatchConstants.BATCH_SIZE);

        if (batch.isEmpty()) {
            return;
        }

        List<DriverMatchJob> alive = dropTimeout(batch);

        if (alive.isEmpty()) {
            return;
        }

        List<DriverCandidate> candidates = driverCandidateFinder.find(alive);

        if (candidates.isEmpty()) {
            requeueAll(alive);
            log.info("本批 {} 单没有候选司机，放回队列", alive.size());
            return;
        }

        double[][] weights = weights(alive, candidates);

        log.info("批次 {} 单 × {} 司机，权值矩阵 {}×{}", alive.size(), candidates.size(),
                weights.length, weights.length == 0 ? 0 : weights[0].length);

        List<DriverMatchResult> results = driverMatcher.match(alive, candidates, weights);

        if (results.isEmpty()) {
            requeueAll(alive);
            return;
        }

        Set<Long> matched = new HashSet<>();
        for (DriverMatchResult result : results) {
            matched.add(result.getJob().getOrderId());
            assign(result);
        }

        List<DriverMatchJob> leftover = new ArrayList<>();
        for (DriverMatchJob job : alive) {
            if (!matched.contains(job.getOrderId())) {
                leftover.add(job);
            }
        }
        requeueAll(leftover);

        log.info("批次匹配完成：成功 {} 单，放回队列 {} 单", matched.size(), leftover.size());
    }

    // 一批订单 × 一批司机 -> 权值矩阵 [订单下标][司机下标]；下标对应入参顺序
    public double[][] weights(List<DriverMatchJob> orders, List<DriverCandidate> drivers) {

        double[][] weights = new double[orders.size()][drivers.size()];

        DriverCandidate probe = new DriverCandidate();

        for (int i = 0; i < orders.size(); i++) {
            DriverMatchJob order = orders.get(i);
            for (int j = 0; j < drivers.size(); j++) {
                DriverCandidate driver = drivers.get(j);

                probe.setDriverId(driver.getDriverId());
                probe.setLng(driver.getLng());
                probe.setLat(driver.getLat());
                probe.setIdleMillis(driver.getIdleMillis());
                probe.setScore(driver.getScore());
                probe.setDistance(GeoUtil.getDistance(
                        order.getStartLng(), order.getStartLat(), driver.getLng(), driver.getLat()));

                weights[i][j] = order.getMetric().score(probe);
            }
        }

        return weights;
    }


    // 在队列里等太久，判定为没匹配到：发消息，不再回队
    List<DriverMatchJob> dropTimeout(List<DriverMatchJob> batch) {

        long now = System.currentTimeMillis();
        List<DriverMatchJob> alive = new ArrayList<>(batch.size());

        for (DriverMatchJob job : batch) {
            if (now - job.getEnqueueAt() > DriverMatchConstants.MATCH_DEADLINE_MS) {
                timeout(job);
            } else {
                alive.add(job);
            }
        }

        return alive;
    }

    void timeout(DriverMatchJob job) {

        eventPublisher.publishEvent(new DriverMatchTimeoutMessage(job.getOrderId(), job.getPassengerId()));

        log.info("订单 {} 超过 {}ms 没匹配到司机，转超时处理", job.getOrderId(),
                DriverMatchConstants.MATCH_DEADLINE_MS);
    }

    // 选中司机之后：交给派单环节落库
    void assign(DriverMatchResult result) {

        DispatchData data = new DispatchData();
        data.setDriverId(result.getDriver().getDriverId());

        DriverMatchJob job = result.getJob();

        eventPublisher.publishEvent(new DispatchMessage(job.getOrderId(), job.getPassengerId(),
                DispatchMessage.Step.DRIVER, true, data));

        log.info("订单 {} 选中司机 {} 权值 {}", job.getOrderId(), data.getDriverId(), result.getWeight());
    }

    private void requeueAll(List<DriverMatchJob> jobs) {
        for (DriverMatchJob job : jobs) {
            driverMatchQueue.requeue(job);
        }
    }
}