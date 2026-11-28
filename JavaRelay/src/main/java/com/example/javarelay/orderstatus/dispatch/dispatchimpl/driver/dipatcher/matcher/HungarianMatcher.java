package com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.dipatcher.matcher;

import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.pojo.DriverCandidate;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.pojo.DriverMatchJob;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.pojo.DriverMatchResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class HungarianMatcher implements DriverMatcher {

    // 权值越高越好，匈牙利求最小代价，所以转成 cost = maxW - weight
    // 矩阵补成方阵：补出来的行列 cost = maxW，等价于权值 0，不会被优先选中
    @Override
    public List<DriverMatchResult> match(List<DriverMatchJob> orders, List<DriverCandidate> drivers,
                                         double[][] weights) {

        if (orders == null || drivers == null || orders.isEmpty() || drivers.isEmpty()) {
            return List.of();
        }

        int n = orders.size();
        int m = drivers.size();
        int size = Math.max(n, m);

        double maxW = 0;
        for (double[] row : weights) {
            for (double w : row) {
                if (w > maxW) {
                    maxW = w;
                }
            }
        }

        double[][] cost = new double[size][size];
        for (double[] row : cost) {
            Arrays.fill(row, maxW);
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                cost[i][j] = maxW - weights[i][j];
            }
        }

        int[] rowToCol = solve(cost);

        List<DriverMatchResult> results = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            int j = rowToCol[i];
            if (j < 0 || j >= m) {
                continue;
            }
            if (weights[i][j] <= 0) {
                continue;
            }

            DriverMatchResult result = new DriverMatchResult();
            result.setJob(orders.get(i));
            result.setDriver(drivers.get(j));
            result.setWeight(weights[i][j]);
            results.add(result);
        }

        return results;
    }

    // 匈牙利算法（O(n^3)）：返回每一行配到的列
    static int[] solve(double[][] cost) {

        int n = cost.length;

        double[] u = new double[n + 1];
        double[] v = new double[n + 1];
        int[] p = new int[n + 1];
        int[] way = new int[n + 1];

        for (int i = 1; i <= n; i++) {

            p[0] = i;
            int j0 = 0;

            double[] minv = new double[n + 1];
            boolean[] used = new boolean[n + 1];
            Arrays.fill(minv, Double.MAX_VALUE);

            do {
                used[j0] = true;

                int i0 = p[j0];
                int j1 = 0;
                double delta = Double.MAX_VALUE;

                for (int j = 1; j <= n; j++) {
                    if (used[j]) {
                        continue;
                    }
                    double cur = cost[i0 - 1][j - 1] - u[i0] - v[j];
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

        int[] rowToCol = new int[n];
        for (int j = 1; j <= n; j++) {
            rowToCol[p[j] - 1] = j - 1;
        }
        return rowToCol;
    }
}