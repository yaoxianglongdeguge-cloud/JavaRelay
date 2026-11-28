package com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.dipatcher;

import com.example.javarelay.pojo.DriverIdle;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.pojo.DriverCandidate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.geo.Point;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DriverCandidateFinderTest {

    private static DriverIdle driver(long id, LocalDateTime updateTime) {
        DriverIdle d = new DriverIdle();
        d.setDriverId(id);
        d.setUpdateTime(updateTime);
        d.setScore(4.0);
        return d;
    }

    @Test
    @DisplayName("只保留 空闲 + 新鲜 + 有坐标 的司机")
    void assemble() {

        DriverCandidateFinder finder = new DriverCandidateFinder();

        long now = System.currentTimeMillis();

        List<DriverIdle> idle = List.of(
                driver(101, LocalDateTime.now().minusMinutes(5)),   // 正常
                driver(102, LocalDateTime.now().minusMinutes(5)),   // 心跳过期
                driver(103, LocalDateTime.now().minusMinutes(5)),   // 没有坐标
                driver(104, LocalDateTime.now().minusSeconds(30))); // 正常

        Set<Long> fresh = Set.of(101L, 103L, 104L);

        Map<Long, Point> positions = new HashMap<>();
        positions.put(101L, new Point(120.15, 30.28));
        positions.put(102L, new Point(120.16, 30.29));
        positions.put(104L, new Point(120.17, 30.30));

        List<DriverCandidate> candidates = finder.assemble(idle, fresh, positions, now);

        assertEquals(2, candidates.size());

        DriverCandidate first = candidates.get(0);
        assertEquals(101L, first.getDriverId());
        assertEquals(120.15, first.getLng(), 1e-9);
        assertEquals(4.0, first.getScore(), 1e-9);
        assertEquals(30.28, first.getLat(), 1e-9);

        DriverCandidate second = candidates.get(1);
        assertEquals(104L, second.getDriverId());
        assertEquals(30_000L, second.getIdleMillis(), 2_000L);
    }

    @Test
    @DisplayName("空输入")
    void empty() {
        DriverCandidateFinder finder = new DriverCandidateFinder();
        assertEquals(0, finder.assemble(List.of(), Set.of(), Map.of(), 0L).size());
    }
}