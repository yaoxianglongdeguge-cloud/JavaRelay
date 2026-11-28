package com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.dipatcher.matcher;

import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.pojo.DriverCandidate;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.pojo.DriverMatchJob;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.pojo.DriverMatchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HungarianMatcherTest {

    private final HungarianMatcher matcher = new HungarianMatcher();

    private static List<DriverMatchJob> orders(int n) {
        return java.util.stream.LongStream.range(1, n + 1)
                .mapToObj(i -> {
                    DriverMatchJob job = new DriverMatchJob();
                    job.setOrderId(i);
                    job.setPassengerId(i);
                    return job;
                })
                .toList();
    }

    private static List<DriverCandidate> drivers(long... ids) {
        return java.util.Arrays.stream(ids)
                .mapToObj(id -> {
                    DriverCandidate c = new DriverCandidate();
                    c.setDriverId(id);
                    return c;
                })
                .toList();
    }

    @Test
    @DisplayName("1 单 1 司机")
    void single() {

        List<DriverMatchResult> results = matcher.match(
                orders(1), drivers(101), new double[][]{{0.9}});

        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getJob().getOrderId());
        assertEquals(101L, results.get(0).getDriver().getDriverId());
        assertEquals(0.9, results.get(0).getWeight(), 1e-9);
    }

    @Test
    @DisplayName("不贪心：整体最优而非单点最大")
    void globalOptimum() {

        // 贪心会先抢 (1,101)=0.9，剩下 (2,102)=0.1，总分 1.0
        // 最优是 (1,102)=0.8 + (2,101)=0.85，总分 1.65
        double[][] weights = {
                {0.9, 0.8},
                {0.85, 0.1}
        };

        List<DriverMatchResult> results = matcher.match(orders(2), drivers(101, 102), weights);

        assertEquals(2, results.size());

        long order1Driver = results.stream()
                .filter(r -> r.getJob().getOrderId() == 1L)
                .findFirst().orElseThrow().getDriver().getDriverId();
        long order2Driver = results.stream()
                .filter(r -> r.getJob().getOrderId() == 2L)
                .findFirst().orElseThrow().getDriver().getDriverId();

        assertEquals(102L, order1Driver);
        assertEquals(101L, order2Driver);
    }

    @Test
    @DisplayName("权值 0 不派")
    void zeroWeightNotAssigned() {

        double[][] weights = {{0.0, 0.0}};

        assertEquals(0, matcher.match(orders(1), drivers(101, 102), weights).size());
    }

    @Test
    @DisplayName("部分可用：只派有权值的")
    void partial() {

        double[][] weights = {
                {0.9, 0.0},
                {0.0, 0.0}
        };

        List<DriverMatchResult> results = matcher.match(orders(2), drivers(101, 102), weights);

        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getJob().getOrderId());
        assertEquals(101L, results.get(0).getDriver().getDriverId());
    }

    @Test
    @DisplayName("司机比订单多：每单一个，司机不重复")
    void moreDriversThanOrders() {

        double[][] weights = {
                {0.5, 0.9, 0.4},
                {0.8, 0.3, 0.6}
        };

        List<DriverMatchResult> results = matcher.match(orders(2), drivers(101, 102, 103), weights);

        assertEquals(2, results.size());

        long distinct = results.stream().map(r -> r.getDriver().getDriverId()).distinct().count();
        assertEquals(2, distinct);

        assertEquals(102L, results.stream().filter(r -> r.getJob().getOrderId() == 1L)
                .findFirst().orElseThrow().getDriver().getDriverId());
        assertEquals(101L, results.stream().filter(r -> r.getJob().getOrderId() == 2L)
                .findFirst().orElseThrow().getDriver().getDriverId());
    }

    @Test
    @DisplayName("订单比司机多：只有司机数量的匹配")
    void moreOrdersThanDrivers() {

        double[][] weights = {
                {0.9},
                {0.8},
                {0.7}
        };

        List<DriverMatchResult> results = matcher.match(orders(3), drivers(101), weights);

        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getJob().getOrderId());
        assertEquals(101L, results.get(0).getDriver().getDriverId());
    }

    @Test
    @DisplayName("空输入")
    void empty() {
        assertTrue(matcher.match(List.of(), drivers(101), new double[0][1]).isEmpty());
        assertTrue(matcher.match(orders(1), List.of(), new double[1][0]).isEmpty());
    }
}