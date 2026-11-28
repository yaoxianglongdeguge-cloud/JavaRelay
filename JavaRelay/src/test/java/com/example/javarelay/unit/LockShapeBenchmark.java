package com.example.javarelay.unit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 对照基准：现状的「条件 INSERT」 vs 方案 B 的「FOR UPDATE + SELECT + INSERT」。
 * 只测耗时与并发错误率，不做断言（除超时）。
 */
class LockShapeBenchmark {

    private static final String URL = System.getProperty("test.db.url",
            "jdbc:mysql://10.66.66.2:3306/javarelay?useUnicode=true&characterEncoding=utf-8"
                    + "&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false");
    private static final String USER = System.getProperty("test.db.user", "javarelay");
    private static final String PASS = System.getProperty("test.db.pass", "wadsprq5221");

    private static final long BASE = 995_000L;

    private static final String SQL_CONDITIONAL_INSERT =
            "INSERT INTO order_node (order_id, node_key) "
                    + "SELECT ?, ? FROM DUAL "
                    + "WHERE NOT EXISTS (SELECT 1 FROM order_node WHERE order_id = ? AND node_key = ?) "
                    + "  AND NOT EXISTS (SELECT 1 FROM order_node WHERE order_id = ? AND node_key = 'FROZEN') "
                    + "  AND (SELECT COUNT(*) FROM order_node WHERE order_id = ? AND node_key IN ('CREATE')) = 1";

    private static final String SQL_LOCK_ORDER =
            "SELECT id, frozen_at FROM orders WHERE id = ? FOR UPDATE";

    private static final String SQL_SELECT_KEYS =
            "SELECT node_key FROM order_node WHERE order_id = ?";

    private static final String SQL_PLAIN_INSERT =
            "INSERT INTO order_node (order_id, node_key) VALUES (?, ?)";

    private static Connection open() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    private static void cleanup() throws SQLException {
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement("DELETE FROM order_node WHERE order_id >= ?")) {
            ps.setLong(1, BASE);
            ps.executeUpdate();
        }
    }

    /** 现状：一条条件插入（需要 CREATE 已存在） */
    private static int conditionalInsert(Connection c, long orderId) throws SQLException {
        c.setAutoCommit(false);
        try (PreparedStatement ps = c.prepareStatement(SQL_CONDITIONAL_INSERT)) {
            ps.setLong(1, orderId);
            ps.setString(2, "DISPATCH");
            ps.setLong(3, orderId);
            ps.setString(4, "DISPATCH");
            ps.setLong(5, orderId);
            ps.setLong(6, orderId);
            int r = ps.executeUpdate();
            c.commit();
            return r;
        } catch (SQLException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(true);
        }
    }

    /** 方案 B：锁订单行 → 读节点 → 单行插入 */
    private static int lockThenInsert(Connection c, long orderId) throws SQLException {

        c.setAutoCommit(false);
        try {
            return lockThenInsertTx(c, orderId);
        } catch (SQLException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(true);
        }
    }

    private static int lockThenInsertTx(Connection c, long orderId) throws SQLException {

        try (PreparedStatement ps = c.prepareStatement(SQL_LOCK_ORDER)) {
            ps.setLong(1, orderId);
            ps.executeQuery().close();
        }

        boolean done = false;
        try (PreparedStatement ps = c.prepareStatement(SQL_SELECT_KEYS)) {
            ps.setLong(1, orderId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    if ("DISPATCH".equals(rs.getString(1))) {
                        done = true;
                    }
                }
            }
        }
        if (done) {
            return 0;
        }

        try (PreparedStatement ps = c.prepareStatement(SQL_PLAIN_INSERT)) {
            ps.setLong(1, orderId);
            ps.setString(2, "DISPATCH");
            int r = ps.executeUpdate();
            c.commit();
            return r;
        }
    }

    private static long[] benchSequential(String label, boolean useB, int rounds) throws SQLException {

        long[] result = new long[3];

        try (Connection c = open()) {
            c.setAutoCommit(true);

            long start = System.nanoTime();
            for (int i = 0; i < rounds; i++) {
                long orderId = BASE + 100_000 + i;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO order_node (order_id, node_key) VALUES (?, 'CREATE')")) {
                    ps.setLong(1, orderId);
                    ps.executeUpdate();
                }
            }
            result[2] = System.nanoTime() - start;

            start = System.nanoTime();
            for (int i = 0; i < rounds; i++) {
                long orderId = BASE + 100_000 + i;
                if (useB) {
                    lockThenInsert(c, orderId);
                } else {
                    conditionalInsert(c, orderId);
                }
            }
            result[0] = System.nanoTime() - start;
            result[1] = rounds;
        }

        System.out.printf("  [单线程] %-8s %d 次：%.2f ms/次  (建 CREATE 共 %.1f ms)%n",
                label, rounds, result[0] / 1_000_000.0 / rounds, result[2] / 1_000_000.0);

        return result;
    }

    private static void benchConcurrent(String label, boolean useB, int orders, int threadsPerOrder) throws Exception {

        int total = orders * threadsPerOrder;
        ExecutorService pool = Executors.newFixedThreadPool(total);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(total);

        AtomicInteger ok = new AtomicInteger();
        AtomicInteger deadlock = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();
        AtomicInteger dup = new AtomicInteger();

        // 先给每个订单建 CREATE
        try (Connection c = open()) {
            for (int i = 0; i < orders; i++) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO order_node (order_id, node_key) VALUES (?, 'CREATE')")) {
                    ps.setLong(1, BASE + 200_000 + i);
                    ps.executeUpdate();
                }
            }
        }

        long t0 = System.nanoTime();

        for (int i = 0; i < orders; i++) {
            final long orderId = BASE + 200_000 + i;
            for (int k = 0; k < threadsPerOrder; k++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        try (Connection c = open()) {
                            c.setAutoCommit(true);
                            int r = useB ? lockThenInsert(c, orderId) : conditionalInsert(c, orderId);
                            if (r == 1) {
                                ok.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        String m = e.getMessage() == null ? "" : e.getMessage();
                        if (m.contains("Deadlock")) {
                            deadlock.incrementAndGet();
                        } else if (m.contains("Duplicate")) {
                            dup.incrementAndGet();
                        } else {
                            other.incrementAndGet();
                        }
                    } finally {
                        done.countDown();
                    }
                });
            }
        }

        start.countDown();
        assertTrue(done.await(120, TimeUnit.SECONDS), "并发基准超时");
        pool.shutdown();

        long ms = (System.nanoTime() - t0) / 1_000_000;

        System.out.printf("  [并发]   %-8s %d 单 × %d 线程：成功=%d 死锁=%d 重复键=%d 其他=%d  总耗时=%d ms%n",
                label, orders, threadsPerOrder, ok.get(), deadlock.get(), dup.get(), other.get(), ms);
    }

    @BeforeAll
    static void before() throws SQLException {
        cleanup();
    }

    @AfterAll
    static void after() throws SQLException {
        cleanup();
    }

    @Test
    @DisplayName("对照基准")
    void benchmark() throws Exception {

        System.out.println();
        System.out.println("========== 单线程（无并发，纯看 SQL 开销）==========");
        benchSequential("现状", false, 500);
        cleanup();
        benchSequential("方案B", true, 500);

        cleanup();
        System.out.println("========== 并发（同一节点多人抢）==========");
        benchConcurrent("现状", false, 10, 4);
        cleanup();
        benchConcurrent("方案B", true, 10, 4);

        cleanup();
        System.out.println("========== 并发（10 单互不相干，各 2 线程）==========");
        benchConcurrent("现状", false, 10, 2);
        cleanup();
        benchConcurrent("方案B", true, 10, 2);

        System.out.println();
    }
}
