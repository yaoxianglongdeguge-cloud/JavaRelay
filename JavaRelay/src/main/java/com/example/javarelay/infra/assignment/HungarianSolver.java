package com.example.javarelay.infra.assignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 匈牙利算法（Kuhn-Munkres，O(n^3)）—— 指派问题通用求解器。
 *
 * <p>支持：
 * <ul>
 *   <li>矩形矩阵（行数可以 != 列数），内部补虚拟节点转方阵</li>
 *   <li>最小成本 {@link #solveMin} / 最大收益 {@link #solveMax}</li>
 *   <li>不可行边：用 {@code allowed[i][j] = false} 标记，结果为"不分配"</li>
 * </ul>
 *
 * <p>约定：行 = 订单，列 = 司机。
 * <ul>
 *   <li>{@code orderToDriver[i] == -1} 表示第 i 个订单没有分配到司机</li>
 *   <li>{@code driverToOrder[j] == -1} 表示第 j 个司机空闲</li>
 * </ul>
 *
 * <p>无状态、线程安全。
 */
public final class HungarianSolver {

    /** 单个成本的绝对值上限，防止内部运算溢出 */
    private static final long MAX_ABS_COST = 1_000_000_000_000L;

    private HungarianSolver() {
    }

    /** 全部边可行的最小成本指派 */
    public static Result solveMin(long[][] cost) {
        return solve(cost, null, false);
    }

    /** 带可行性约束的最小成本指派 */
    public static Result solveMin(long[][] cost, boolean[][] allowed) {
        return solve(cost, allowed, false);
    }

    /** 全部边可行的最大收益指派 */
    public static Result solveMax(long[][] profit) {
        return solve(profit, null, true);
    }

    /** 带可行性约束的最大收益指派 */
    public static Result solveMax(long[][] profit, boolean[][] allowed) {
        return solve(profit, allowed, true);
    }

    // ------------------------------------------------------------------ 核心

    private static Result solve(long[][] input, boolean[][] allowed, boolean maximize) {

        if (input == null || input.length == 0) {
            return Result.empty(0, 0);
        }

        int rows = input.length;
        int cols = input[0] == null ? 0 : input[0].length;

        if (cols == 0) {
            return Result.empty(rows, 0);
        }

        if (allowed != null) {
            if (allowed.length != rows) {
                throw new IllegalArgumentException("allowed 行数与 cost 不一致");
            }
            for (int i = 0; i < rows; i++) {
                if (allowed[i] == null || allowed[i].length != cols) {
                    throw new IllegalArgumentException("allowed 第 " + i + " 行长度异常");
                }
            }
        }

        // 1) 转成内部"成本矩阵"：最大化时取负
        long[][] cost = new long[rows][cols];
        long maxAbs = 0;

        for (int i = 0; i < rows; i++) {
            if (input[i] == null || input[i].length != cols) {
                throw new IllegalArgumentException("矩阵不是矩形：第 " + i + " 行长度异常");
            }
            for (int j = 0; j < cols; j++) {
                if (allowed != null && !allowed[i][j]) {
                    cost[i][j] = 0;                       // 占位，后面统一替换成 big
                    continue;
                }
                long v = maximize ? -input[i][j] : input[i][j];
                if (Math.abs(v) > MAX_ABS_COST) {
                    throw new IllegalArgumentException("成本超出允许范围（|v| <= 1e12）：" + v);
                }
                cost[i][j] = v;
                maxAbs = Math.max(maxAbs, Math.abs(v));
            }
        }

        // 2) 补虚拟节点：把"配不上"变成一种合法的匹配结果
        int n = Math.max(rows, cols);
        long big = maxAbs * (n + 1) + 1;
        long[][] a = new long[n][n];
        for (long[] row : a) {
            Arrays.fill(row, big);
        }

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                boolean feasible = allowed == null || allowed[i][j];
                a[i][j] = feasible ? cost[i][j] : big;
            }
        }

        // 3) 平移成非负（算法要求成本非负，平移不改变最优指派）
        long min = Long.MAX_VALUE;
        for (long[] row : a) {
            for (long x : row) {
                min = Math.min(min, x);
            }
        }
        if (min != 0) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    a[i][j] -= min;
                }
            }
        }

        // 4) 求解
        int[] assign = km(a);

        // 5) 还原：只认"真实订单 + 真实司机 + 允许"的配对
        int[] orderToDriver = new int[rows];
        Arrays.fill(orderToDriver, -1);
        int[] driverToOrder = new int[cols];
        Arrays.fill(driverToOrder, -1);

        int matched = 0;
        for (int j = 0; j < n; j++) {
            int i = assign[j];
            if (i < 0 || i >= rows || j >= cols) {
                continue;
            }
            if (allowed != null && !allowed[i][j]) {
                continue;
            }
            orderToDriver[i] = j;
            driverToOrder[j] = i;
            matched++;
        }

        long total = 0;
        for (int i = 0; i < rows; i++) {
            if (orderToDriver[i] >= 0) {
                total += input[i][orderToDriver[i]];
            }
        }

        return new Result(orderToDriver, driverToOrder, total, matched, rows - matched);
    }

    /**
     * 方阵最小成本完美匹配（1-based 内部实现）。
     *
     * @param a n*n 非负成本矩阵
     * @return assign[司机下标] = 订单下标
     */
    private static int[] km(long[][] a) {

        int n = a.length;

        long[] u = new long[n + 1];        // 订单顶标
        long[] v = new long[n + 1];        // 司机顶标
        int[] p = new int[n + 1];          // p[j] = 匹配到司机 j 的订单（1-based，0 表示未匹配）
        int[] way = new int[n + 1];        // 回溯用

        for (int i = 1; i <= n; i++) {

            p[0] = i;
            int j0 = 0;

            long[] minv = new long[n + 1];
            Arrays.fill(minv, Long.MAX_VALUE);
            boolean[] used = new boolean[n + 1];

            do {
                used[j0] = true;

                int i0 = p[j0];
                int j1 = -1;
                long delta = Long.MAX_VALUE;

                for (int j = 1; j <= n; j++) {
                    if (used[j]) {
                        continue;
                    }
                    long cur = a[i0 - 1][j - 1] - u[i0] - v[j];
                    if (cur < minv[j]) {
                        minv[j] = cur;
                        way[j] = j0;
                    }
                    if (minv[j] < delta) {
                        delta = minv[j];
                        j1 = j;
                    }
                }

                for (int j = 0; j <= n; j++) {
                    if (used[j]) {
                        u[p[j]] += delta;
                        v[j] -= delta;
                    } else {
                        minv[j] -= delta;
                    }
                }

                j0 = j1;

            } while (p[j0] != 0);

            do {
                int j1 = way[j0];
                p[j0] = p[j1];
                j0 = j1;
            } while (j0 != 0);
        }

        int[] assign = new int[n];
        Arrays.fill(assign, -1);
        for (int j = 1; j <= n; j++) {
            if (p[j] != 0) {
                assign[j - 1] = p[j] - 1;
            }
        }
        return assign;
    }

    // ------------------------------------------------------------------ 结果

    /** 指派结果 */
    public static final class Result {

        private final int[] orderToDriver;
        private final int[] driverToOrder;
        private final long totalValue;
        private final int matchedCount;
        private final int unassignedOrderCount;

        private Result(int[] orderToDriver, int[] driverToOrder,
                       long totalValue, int matchedCount, int unassignedOrderCount) {
            this.orderToDriver = orderToDriver;
            this.driverToOrder = driverToOrder;
            this.totalValue = totalValue;
            this.matchedCount = matchedCount;
            this.unassignedOrderCount = unassignedOrderCount;
        }

        static Result empty(int rows, int cols) {
            int[] o2d = new int[rows];
            Arrays.fill(o2d, -1);
            int[] d2o = new int[cols];
            Arrays.fill(d2o, -1);
            return new Result(o2d, d2o, 0L, 0, rows);
        }

        /** 第 i 个订单分配的司机下标，-1 表示未分配 */
        public int driverOf(int orderIndex) {
            return orderToDriver[orderIndex];
        }

        /** 第 j 个司机接的订单下标，-1 表示空闲 */
        public int orderOf(int driverIndex) {
            return driverToOrder[driverIndex];
        }

        public boolean isMatched(int orderIndex) {
            return orderToDriver[orderIndex] >= 0;
        }

        /** solveMin 时为总成本；solveMax 时为总收益 */
        public long getTotalValue() {
            return totalValue;
        }

        public int getMatchedCount() {
            return matchedCount;
        }

        public int getUnassignedOrderCount() {
            return unassignedOrderCount;
        }

        public int[] orderToDriver() {
            return orderToDriver.clone();
        }

        public int[] driverToOrder() {
            return driverToOrder.clone();
        }

        /** 所有真实配对 */
        public List<Pair> pairs() {
            List<Pair> list = new ArrayList<>(matchedCount);
            for (int i = 0; i < orderToDriver.length; i++) {
                if (orderToDriver[i] >= 0) {
                    list.add(new Pair(i, orderToDriver[i]));
                }
            }
            return list;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Result{matched=")
                    .append(matchedCount)
                    .append(", unassigned=")
                    .append(unassignedOrderCount)
                    .append(", total=")
                    .append(totalValue)
                    .append(", pairs=[");
            boolean first = true;
            for (Pair p : pairs()) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                sb.append(p.getOrderIndex()).append("->").append(p.getDriverIndex());
            }
            return sb.append("]}").toString();
        }
    }

    /** 一个配对 */
    public static final class Pair {

        private final int orderIndex;
        private final int driverIndex;

        public Pair(int orderIndex, int driverIndex) {
            this.orderIndex = orderIndex;
            this.driverIndex = driverIndex;
        }

        public int getOrderIndex() {
            return orderIndex;
        }

        public int getDriverIndex() {
            return driverIndex;
        }
    }
}