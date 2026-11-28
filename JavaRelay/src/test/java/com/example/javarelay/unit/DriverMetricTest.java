package com.example.javarelay.unit;

import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.metric.impl.DistanceMetric;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.pojo.DriverCandidate;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.metric.DriverMetric;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.metric.impl.IdleMetric;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.metric.impl.ScoreMetric;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DriverMetricTest {

    private static DriverCandidate candidate(double distance, long idleMillis) {
        DriverCandidate c = new DriverCandidate();
        c.setDriverId(1L);
        c.setDistance(distance);
        c.setIdleMillis(idleMillis);
        return c;
    }

    @Test
    @DisplayName("distance")
    void distanceMetric() {
        DistanceMetric m = new DistanceMetric();
        m.setMaxDistance(1000);

        assertEquals(1.0, m.score(candidate(0, 0)), 1e-9);
        assertEquals(0.5, m.score(candidate(500, 0)), 1e-9);
        assertEquals(0.0, m.score(candidate(1000, 0)), 1e-9);
        assertEquals(0.0, m.score(candidate(2000, 0)), 1e-9);
        assertEquals("default", m.type());
    }

    @Test
    @DisplayName("distance weight")
    void distanceWeight() {
        DistanceMetric m = new DistanceMetric();
        m.setMaxDistance(1000);
        m.setWeight(2.0);

        assertEquals(1.0, m.score(candidate(500, 0)), 1e-9);
    }

    @Test
    @DisplayName("idle")
    void idleMetric() {
        IdleMetric m = new IdleMetric();
        m.setCapMillis(1000);

        assertEquals(0.0, m.score(candidate(0, 0)), 1e-9);
        assertEquals(0.5, m.score(candidate(0, 500)), 1e-9);
        assertEquals(1.0, m.score(candidate(0, 1000)), 1e-9);
        assertEquals(1.0, m.score(candidate(0, 5000)), 1e-9);
        assertEquals("idle", m.type());
    }

    @Test
    @DisplayName("越近越高")
    void nearerScoresHigher() {
        DriverMetric m = new DistanceMetric();
        DriverCandidate near = candidate(100, 0);
        DriverCandidate far = candidate(800, 0);

        assertTrue(m.score(near) > m.score(far));
    }

    @Test
    @DisplayName("score")
    void scoreMetric() {
        ScoreMetric m = new ScoreMetric();
        m.setMinScore(3.0);
        m.setMaxScore(5.0);

        DriverCandidate c = new DriverCandidate();

        c.setScore(2.0);
        assertEquals(0.0, m.score(c), 1e-9);

        c.setScore(3.0);
        assertEquals(0.0, m.score(c), 1e-9);

        c.setScore(4.0);
        assertEquals(0.5, m.score(c), 1e-9);

        c.setScore(5.0);
        assertEquals(1.0, m.score(c), 1e-9);

        c.setScore(6.0);
        assertEquals(1.0, m.score(c), 1e-9);

        assertEquals("score", m.type());
    }
}