package com.example.javarelay.orderstatus.dispatch;

import com.example.javarelay.dto.Result;
import com.example.javarelay.orderstatus.service.DispatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 派单执行器：把求解结果逐个落库，返回没派成的订单。
 *
 * <p>失败原因（司机被抢、订单状态已变）都视为"这一轮没派成"，交给下一轮。
 */
@Slf4j
@Component
public class DispatchExecutor {

    @Autowired
    DispatchService dispatchService;

    /**
     * @return 没派成功的 orderId 集合
     */
    public Set<Long> execute(List<DispatchJob> jobs, DispatchMatrix matrix, DispatchSolver.Solution solution) {

        Set<Long> failedOrderIds = new HashSet<>();

        for (int i = 0; i < jobs.size(); i++) {

            DispatchJob job = jobs.get(i);

            int column = solution.driverIndexOf(i);

            if (column < 0) {
                failedOrderIds.add(job.getOrderId());
                continue;
            }

            Long driverId = matrix.getDriverIds().get(column);

            Result result;

            try {
                result = dispatchService.applyAssign(job.getOrderId(), job.getPassengerId(), driverId);
            } catch (Exception e) {
                log.error("派单异常 orderId={} driverId={}", job.getOrderId(), driverId, e);
                failedOrderIds.add(job.getOrderId());
                continue;
            }

            if (result == null || result.getCode() != 200) {
                log.warn("派单未成功 orderId={} driverId={} reason={}",
                        job.getOrderId(), driverId, result == null ? "null" : result.getMessage());
                failedOrderIds.add(job.getOrderId());
            }
        }

        return failedOrderIds;
    }
}