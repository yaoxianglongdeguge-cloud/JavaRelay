package com.example.javarelay.unit;

import com.example.javarelay.orderstatus.node.OrderNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptimisticLockTest {

    private static final String URL = System.getProperty("test.db.url",
            "jdbc:mysql://10.66.66.2:3306/javarelay?useUnicode=true&characterEncoding=utf-8"
                    + "&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false");
    private static final String USER = System.getProperty("test.db.user", "javarelay");
    private static final String PASS = System.getProperty("test.db.pass", "wadsprq5221");

    private static final long BASE = 990_000L;

    enum R { EXECUTED, ALREADY_DONE, NOT_READY }

    private static long seq = 0;

    private static Connection open() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    private static void cleanup() throws SQLException {
        try (Connection c = open()) {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM order_node WHERE order_id >= ?")) {
                ps.setLong(1, BASE);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM orders WHERE id >= ?")) {
                ps.setLong(1, BASE);
                ps.executeUpdate();
            }
        }
    }

    private static long newOrder() throws SQLException {
        long id = BASE + (++seq);
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO orders (id, passenger_id, status, active_flag) VALUES (?, ?, 0, NULL)")) {
            ps.setLong(1, id);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
        return id;
    }

    private static R advance(Connection c, long orderId, OrderNode node) throws SQLException {

        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, frozen_at FROM orders WHERE id = ? FOR UPDATE")) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return R.NOT_READY;
                }
                if (rs.getTimestamp("frozen_at") != null) {
                    return R.NOT_READY;
                }
            }
        }

        Set<String> done = new HashSet<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT node_key FROM order_node WHERE order_id = ?")) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    done.add(rs.getString(1));
                }
            }
        }

        if (done.contains(node.name())) {
            return R.ALREADY_DONE;
        }
        for (OrderNode p : node.directPre()) {
            if (!done.contains(p.name())) {
                return R.NOT_READY;
            }
        }

        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO order_node (order_id, node_key) VALUES (?, ?)")) {
            ps.setLong(1, orderId);
            ps.setString(2, node.name());
            ps.executeUpdate();
            return R.EXECUTED;
        } catch (SQLIntegrityConstraintViolationException e) {
            return R.ALREADY_DONE;
        }
    }

    private static R run(long orderId, OrderNode node) throws SQLException {
        try (Connection c = open()) {
            c.setAutoCommit(false);
            try {
                R r = advance(c, orderId, node);
                c.commit();
                return r;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }


        private static R runFreeze(long orderId) throws SQLException {
        try (Connection c = open()) {
            c.setAutoCommit(false);
            try {
                R r = advance(c, orderId, OrderNode.FROZEN);
                if (r != R.EXECUTED) {
                    c.rollback();
                    return r;
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE orders SET frozen_at = NOW(), active_flag = NULL WHERE id = ?")) {
                    ps.setLong(1, orderId);
                    ps.executeUpdate();
                }
                c.commit();
                return R.EXECUTED;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }    private static int countNode(long orderId, OrderNode node) throws SQLException {
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT COUNT(*) FROM order_node WHERE order_id = ? AND node_key = ?")) {
            ps.setLong(1, orderId);
            ps.setString(2, node.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
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
    @DisplayName("graph")
    void graphStructureIsConsistent() {
        for (OrderNode n : OrderNode.values()) {
            assertFalse(n.descendants().contains(n), n + " cycle");
            for (OrderNode p : n.directPre()) {
                assertTrue(p.descendants().contains(n), p + " missing " + n);
            }
        }
        assertEquals(Set.of(OrderNode.RECEIVE, OrderNode.ARRIVE, OrderNode.FINISH, OrderNode.SETTLE),
                OrderNode.DISPATCH.descendants());
        assertEquals(Set.of(OrderNode.SETTLE), OrderNode.ROUTE_PRICE.descendants());
        assertEquals(Set.of(), OrderNode.FROZEN.descendants());
    }

    @Test
    @DisplayName("firstAdvance")
    void firstAdvance() throws SQLException {
        long id = newOrder();
        assertEquals(R.EXECUTED, run(id, OrderNode.CREATE));
        assertEquals(1, countNode(id, OrderNode.CREATE));
    }

    @Test
    @DisplayName("repeatAdvance")
    void repeatAdvance() throws SQLException {
        long id = newOrder();
        assertEquals(R.EXECUTED, run(id, OrderNode.CREATE));
        assertEquals(R.ALREADY_DONE, run(id, OrderNode.CREATE));
        assertEquals(1, countNode(id, OrderNode.CREATE));
    }

    @Test
    @DisplayName("advanceWithoutPre")
    void advanceWithoutPre() throws SQLException {
        long id = newOrder();
        assertEquals(R.NOT_READY, run(id, OrderNode.RECEIVE));
        assertEquals(0, countNode(id, OrderNode.RECEIVE));
    }

    @Test
    @DisplayName("advanceChain")
    void advanceChain() throws SQLException {
        long id = newOrder();
        assertEquals(R.EXECUTED, run(id, OrderNode.CREATE));
        assertEquals(R.EXECUTED, run(id, OrderNode.DISPATCH));
        assertEquals(R.EXECUTED, run(id, OrderNode.RECEIVE));
        assertEquals(R.EXECUTED, run(id, OrderNode.ARRIVE));
        assertEquals(R.EXECUTED, run(id, OrderNode.FINISH));
    }

    @Test
    @DisplayName("joinNeedsAllPres")
    void joinNeedsAllPres() throws SQLException {
        long id = newOrder();
        assertEquals(R.EXECUTED, run(id, OrderNode.CREATE));
        assertEquals(R.EXECUTED, run(id, OrderNode.ROUTE_PRICE));
        assertEquals(R.NOT_READY, run(id, OrderNode.SETTLE));

        assertEquals(R.EXECUTED, run(id, OrderNode.DISPATCH));
        assertEquals(R.EXECUTED, run(id, OrderNode.RECEIVE));
        assertEquals(R.EXECUTED, run(id, OrderNode.ARRIVE));
        assertEquals(R.EXECUTED, run(id, OrderNode.FINISH));

        assertEquals(R.EXECUTED, run(id, OrderNode.SETTLE));
    }

    @Test
    @DisplayName("routePriceIndependent")
    void routePriceIndependent() throws SQLException {
        long id = newOrder();
        assertEquals(R.EXECUTED, run(id, OrderNode.CREATE));
        assertEquals(R.EXECUTED, run(id, OrderNode.ROUTE_PRICE));
        assertEquals(0, countNode(id, OrderNode.DISPATCH));
    }

    @Test
    @DisplayName("frozenBlocksEverything")
    void frozenBlocksEverything() throws SQLException {
        long id = newOrder();
        assertEquals(R.EXECUTED, run(id, OrderNode.CREATE));
        assertEquals(R.EXECUTED, runFreeze(id));

        assertEquals(R.NOT_READY, run(id, OrderNode.DISPATCH));
        assertEquals(R.NOT_READY, run(id, OrderNode.ROUTE_PRICE));
        assertEquals(R.NOT_READY, run(id, OrderNode.RECEIVE));
        assertEquals(R.NOT_READY, run(id, OrderNode.FROZEN));
    }

    @Test
    @DisplayName("missingOrder")
    void missingOrder() throws SQLException {
        assertEquals(R.NOT_READY, run(BASE + 900_001, OrderNode.CREATE));
    }

    @Test
    @DisplayName("ordersIsolated")
    void ordersIsolated() throws SQLException {
        long a = newOrder();
        long b = newOrder();
        assertEquals(R.EXECUTED, run(a, OrderNode.CREATE));
        assertEquals(R.EXECUTED, run(b, OrderNode.CREATE));
        assertEquals(R.EXECUTED, runFreeze(a));
        assertEquals(R.EXECUTED, run(b, OrderNode.DISPATCH));
    }

    @Test
    @DisplayName("concurrentSameNode")
    void concurrentSameNode() throws Exception {

        long id = newOrder();
        assertEquals(R.EXECUTED, run(id, OrderNode.CREATE));

        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        AtomicInteger executed = new AtomicInteger();
        AtomicInteger already = new AtomicInteger();
        AtomicInteger notReady = new AtomicInteger();
        AtomicInteger error = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    switch (run(id, OrderNode.DISPATCH)) {
                        case EXECUTED -> executed.incrementAndGet();
                        case ALREADY_DONE -> already.incrementAndGet();
                        default -> notReady.incrementAndGet();
                    }
                } catch (Exception e) {
                    if (error.incrementAndGet() == 1) {
                        System.out.println("  first error: " + e.getClass().getSimpleName() + " : " + e.getMessage());
                    }
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(60, TimeUnit.SECONDS));
        pool.shutdown();

        System.out.printf("  20 threads: EXECUTED=%d ALREADY_DONE=%d NOT_READY=%d ERROR=%d%n",
                executed.get(), already.get(), notReady.get(), error.get());

        assertEquals(1, executed.get());
        assertEquals(threads - 1, already.get());
        assertEquals(0, error.get());
        assertEquals(1, countNode(id, OrderNode.DISPATCH));
    }

    @Test
    @DisplayName("concurrentAcrossOrders")
    void concurrentAcrossOrders() throws Exception {

        int n = 10;
        long[] ids = new long[n];
        for (int i = 0; i < n; i++) {
            ids[i] = newOrder();
        }

        ExecutorService pool = Executors.newFixedThreadPool(n * 2);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(n * 2);

        AtomicInteger executed = new AtomicInteger();
        AtomicInteger error = new AtomicInteger();

        for (long id : ids) {
            for (int k = 0; k < 2; k++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        if (run(id, OrderNode.CREATE) == R.EXECUTED) {
                            executed.incrementAndGet();
                        }
                    } catch (Exception e) {
                        if (error.incrementAndGet() == 1) {
                            System.out.println("  first error: " + e.getClass().getSimpleName() + " : " + e.getMessage());
                        }
                    } finally {
                        done.countDown();
                    }
                });
            }
        }

        start.countDown();
        assertTrue(done.await(60, TimeUnit.SECONDS));
        pool.shutdown();

        System.out.printf("  %d orders x 2 threads: EXECUTED=%d ERROR=%d%n", n, executed.get(), error.get());
        assertEquals(n, executed.get());
        assertEquals(0, error.get());

        for (long id : ids) {
            assertEquals(1, countNode(id, OrderNode.CREATE));
        }
    }
}