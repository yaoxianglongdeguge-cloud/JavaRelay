package com.example.javarelay.orderstatus.dispatch;

import com.example.javarelay.infra.assignment.HungarianSolver;
import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * 求解器：把"算法"这一步隔离出来。
 *
 * <p>业务代码只认 {@link Solution}，不直接依赖算法的细节；
 * 以后换算法（比如最小费用流）只改这个类。
 */
@Component
public class DispatchSolver {

    public Solution solve(DispatchMatrix matrix) {

        long start = System.nanoTime();

        HungarianSolver.Result result = HungarianSolver.solveMax(
                matrix.getProfit(), matrix.getAllowed());

        long micros = (System.nanoTime() - start) / 1000L;

        return new Solution(result, micros);
    }

    /** 求解结果 + 耗时 */
    @Getter
    public static class Solution {

        private final HungarianSolver.Result result;

        /** 算法耗时（微秒） */
        private final long micros;

        public Solution(HungarianSolver.Result result, long micros) {
            this.result = result;
            this.micros = micros;
        }

        /** 第 i 个订单分到的司机列下标，-1 表示没配上 */
        public int driverIndexOf(int orderIndex) {
            return result.driverOf(orderIndex);
        }

        public int matchedCount() {
            return result.getMatchedCount();
        }
    }
}