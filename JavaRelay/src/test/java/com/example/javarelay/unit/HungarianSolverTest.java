package com.example.javarelay.unit;

import com.example.javarelay.infra.assignment.HungarianSolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("匈牙利算法求解器")
class HungarianSolverTest {

    @Test
    @DisplayName("贪心陷阱：2x2 最优不是局部最优")
    void greedyTrap() {
        long[][] cost = {
                {1, 2},
                {2, 100}
        };
        HungarianSolver.Result r = HungarianSolver.solveMin(cost);

        assertEquals(4, r.getTotalValue());       // 贪心会得到 101
        assertEquals(1, r.driverOf(0));           // 单0 -> d1
        assertEquals(0, r.driverOf(1));           // 单1 -> d0
        assertEquals(2, r.getMatchedCount());
    }

    @Test
    @DisplayName("3x3 最大收益：手算结果是 11")
    void maxProfit3x3() {
        long[][] profit = {
                {4, 1, 3},
                {2, 0, 5},
                {3, 2, 2}
        };
        HungarianSolver.Result r = HungarianSolver.solveMax(profit);

        assertEquals(11, r.getTotalValue());
        assertEquals(0, r.driverOf(0));
        assertEquals(2, r.driverOf(1));
        assertEquals(1, r.driverOf(2));
    }

    @Test
    @DisplayName("矩形矩阵：3 个订单 5 个司机")
    void rectangularMoreDrivers() {
        long[][] profit = {
                {10, 1, 1, 1, 1},
                {1, 10, 1, 1, 1},
                {1, 1, 10, 1, 1}
        };
        HungarianSolver.Result r = HungarianSolver.solveMax(profit);

        assertEquals(30, r.getTotalValue());
        assertEquals(3, r.getMatchedCount());
        assertEquals(0, r.getUnassignedOrderCount());

        int idle = 0;
        for (int j = 0; j < 5; j++) {
            if (r.orderOf(j) < 0) {
                idle++;
            }
        }
        assertEquals(2, idle);     // 5 个司机里有 2 个空闲
    }

    @Test
    @DisplayName("矩形矩阵：5 个订单 3 个司机，必有订单分配不到")
    void rectangularMoreOrders() {
        long[][] profit = {
                {10, 1, 1},
                {1, 10, 1},
                {1, 1, 10},
                {5, 5, 5},
                {1, 1, 1}
        };
        HungarianSolver.Result r = HungarianSolver.solveMax(profit);

        assertEquals(3, r.getMatchedCount());
        assertEquals(2, r.getUnassignedOrderCount());
    }

    @Test
    @DisplayName("不可行边不会被打上匹配")
    void infeasibleEdges() {
        long[][] cost = {
                {1, 1},
                {1, 1}
        };
        boolean[][] allowed = {
                {true, false},
                {false, true}
        };
        HungarianSolver.Result r = HungarianSolver.solveMin(cost, allowed);

        assertEquals(2, r.getMatchedCount());
        assertEquals(0, r.driverOf(0));
        assertEquals(1, r.driverOf(1));
    }

    @Test
    @DisplayName("某个订单所有边都不可行 -> 该订单不分配")
    void fullyInfeasibleOrder() {
        long[][] cost = {
                {1, 2},
                {3, 4}
        };
        boolean[][] allowed = {
                {true, true},
                {false, false}
        };
        HungarianSolver.Result r = HungarianSolver.solveMin(cost, allowed);

        assertEquals(1, r.getMatchedCount());
        assertEquals(1, r.getUnassignedOrderCount());
        assertFalse(r.isMatched(1));
    }

    @Test
    @DisplayName("全部可行时结果与暴力枚举一致（方阵 min / max）")
    void bruteForceCrossCheck() {
        Random rnd = new Random(20241124L);

        for (int n = 1; n <= 5; n++) {
            for (int iter = 0; iter < 150; iter++) {

                long[][] m = new long[n][n];
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n; j++) {
                        m[i][j] = rnd.nextInt(41) - 20;      // 含负数
                    }
                }

                assertEquals(bruteMin(m), HungarianSolver.solveMin(m).getTotalValue(),
                        "min 不一致 n=" + n);
                assertEquals(bruteMax(m), HungarianSolver.solveMax(m).getTotalValue(),
                        "max 不一致 n=" + n);
            }
        }
    }

    @Test
    @DisplayName("带不可行边的结果也与暴力枚举一致")
    void bruteForceWithInfeasible() {
        Random rnd = new Random(7L);

        for (int n = 2; n <= 5; n++) {
            for (int iter = 0; iter < 100; iter++) {

                long[][] m = new long[n][n];
                boolean[][] allowed = new boolean[n][n];
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n; j++) {
                        m[i][j] = rnd.nextInt(30) + 1;
                        allowed[i][j] = rnd.nextInt(4) != 0;      // 25% 不可行
                    }
                }

                HungarianSolver.Result r = HungarianSolver.solveMin(m, allowed);

                // 对照：先最大化配对数，再最小化总成本
                long[] best = bestPartial(m, allowed);   // best[0]=配对数, best[1]=成本

                assertEquals((int) best[0], r.getMatchedCount(),
                        "配对数不一致 n=" + n);
                assertEquals(best[1], r.getTotalValue(),
                        "带不可行边成本不一致 n=" + n);
            }
        }
    }

    // ---------------------------------------------------------- 暴力参照

    private static long bruteMin(long[][] m) {
        int n = m.length;
        int[] idx = new int[n];
        for (int i = 0; i < n; i++) {
            idx[i] = i;
        }
        long best = Long.MAX_VALUE;
        do {
            long s = 0;
            for (int i = 0; i < n; i++) {
                s += m[i][idx[i]];
            }
            best = Math.min(best, s);
        } while (nextPermutation(idx));
        return best;
    }

    private static long bruteMax(long[][] m) {
        int n = m.length;
        int[] idx = new int[n];
        for (int i = 0; i < n; i++) {
            idx[i] = i;
        }
        long best = Long.MIN_VALUE;
        do {
            long s = 0;
            for (int i = 0; i < n; i++) {
                s += m[i][idx[i]];
            }
            best = Math.max(best, s);
        } while (nextPermutation(idx));
        return best;
    }

    /**
     * 部分匹配的参照解：先最大化配对数，其次最小化总成本。
     * 返回 {配对数, 总成本}
     */
    private static long[] bestPartial(long[][] m, boolean[][] allowed) {
        long[] best = {-1, Long.MAX_VALUE};
        boolean[] usedDriver = new boolean[m.length];
        dfs(m, allowed, 0, usedDriver, 0, 0L, best);
        return best;
    }

    private static void dfs(long[][] m, boolean[][] allowed, int order,
                            boolean[] usedDriver, int count, long cost, long[] best) {

        if (order == m.length) {
            if (count > best[0] || (count == best[0] && cost < best[1])) {
                best[0] = count;
                best[1] = cost;
            }
            return;
        }

        // 选项一：这个订单不分配
        dfs(m, allowed, order + 1, usedDriver, count, cost, best);

        // 选项二：分给某个可行且未使用的司机
        for (int j = 0; j < m.length; j++) {
            if (!allowed[order][j] || usedDriver[j]) {
                continue;
            }
            usedDriver[j] = true;
            dfs(m, allowed, order + 1, usedDriver, count + 1, cost + m[order][j], best);
            usedDriver[j] = false;
        }
    }

    private static boolean nextPermutation(int[] a) {
        int i = a.length - 2;
        while (i >= 0 && a[i] >= a[i + 1]) {
            i--;
        }
        if (i < 0) {
            return false;
        }
        int j = a.length - 1;
        while (a[j] <= a[i]) {
            j--;
        }
        int tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
        for (int l = i + 1, r = a.length - 1; l < r; l++, r--) {
            tmp = a[l];
            a[l] = a[r];
            a[r] = tmp;
        }
        return true;
    }
}