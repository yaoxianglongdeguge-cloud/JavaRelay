package com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.dipatcher;

import com.example.javarelay.pojo.DriverIdle;
import com.example.javarelay.data.mapper.DriverMapper;
import com.example.javarelay.datamanage.service.DriverGeoService;
import com.example.javarelay.dto.Result;
import com.example.javarelay.infra.redis.rediscommon.RedisPefix;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.DriverMatchConstants;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.pojo.DriverCandidate;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.pojo.DriverMatchJob;
import com.example.javarelay.pojo.DriverGeo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class DriverCandidateFinder {

    @Autowired
    DriverGeoService driverGeoService;

    @Autowired
    DriverMapper driverMapper;

    @Autowired
    StringRedisTemplate redisTemplate;

    // 一批订单 -> 一批候选司机：附近 + 空闲 + 位置新鲜
    public List<DriverCandidate> find(List<DriverMatchJob> orders) {

        if (orders == null || orders.isEmpty()) {
            return List.of();
        }

        Set<Long> nearbyIds = new LinkedHashSet<>();

        for (DriverMatchJob order : orders) {
            nearbyIds.addAll(nearby(order));
        }

        if (nearbyIds.isEmpty()) {
            return List.of();
        }

        List<Long> ids = new ArrayList<>(nearbyIds);
        List<DriverIdle> idle = driverMapper.selectIdleByIds(ids);

        if (idle.isEmpty()) {
            return List.of();
        }

        //位置是否新鲜
        Set<Long> fresh = freshIds(idle.stream().map(DriverIdle::getDriverId).toList());

        return assemble(idle, fresh, positions(ids), System.currentTimeMillis());
    }

    List<DriverCandidate> assemble(List<DriverIdle> idle, Set<Long> fresh,
                                   Map<Long, Point> positions, long now) {

        List<DriverCandidate> candidates = new ArrayList<>(idle.size());

        for (DriverIdle driver : idle) {
            if (!fresh.contains(driver.getDriverId())) {
                continue;
            }
            Point point = positions.get(driver.getDriverId());
            if (point == null) {
                continue;
            }

            DriverCandidate candidate = new DriverCandidate();
            candidate.setDriverId(driver.getDriverId());
            candidate.setLng(point.getX());
            candidate.setLat(point.getY());
            candidate.setIdleMillis(idleMillis(driver.getUpdateTime(), now));
            candidate.setScore(driver.getScore() == null ? 0 : driver.getScore());
            candidates.add(candidate);
        }

        return candidates;
    }

    //一个订单起始位置到一批司机距离
    private List<Long> nearby(DriverMatchJob order) {

        Result result;
        try {
            result = driverGeoService.nearby(
                    BigDecimal.valueOf(order.getStartLng()),
                    BigDecimal.valueOf(order.getStartLat()),
                    DriverMatchConstants.CANDIDATE_RADIUS_METERS,
                    DriverMatchConstants.CANDIDATE_LIMIT);
        } catch (Exception e) {
            log.error("附近司机查询失败 orderId={}", order.getOrderId(), e);
            return List.of();
        }

        if (result == null || result.getCode() != 200 || result.getData() == null) {
            return List.of();
        }

        List<DriverGeo> list = (List<DriverGeo>) result.getData();

        List<Long> ids = new ArrayList<>(list.size());
        for (DriverGeo geo : list) {
            if (geo.getDriverId() != null) {
                ids.add(geo.getDriverId());
            }
        }
        return ids;
    }

    // 位置心跳过期的不算候选
    private Set<Long> freshIds(List<Long> ids) {

        List<String> keys = ids.stream()
                .map(id -> RedisPefix.DRIVER_LOC_ALIVE_PREFIX + id)
                .toList();

        List<String> values = redisTemplate.opsForValue().multiGet(keys);

        if (values == null) {
            return Set.of();
        }

        Set<Long> fresh = new LinkedHashSet<>();
        for (int i = 0; i < ids.size(); i++) {
            if (values.get(i) != null) {
                fresh.add(ids.get(i));
            }
        }
        return fresh;
    }

    //司机位置
    private Map<Long, Point> positions(List<Long> ids) {

        String[] members = ids.stream().map(String::valueOf).toArray(String[]::new);

        List<Point> points = redisTemplate.opsForGeo().position(RedisPefix.DRIVER_GEO, members);

        if (points == null) {
            return Map.of();
        }

        Map<Long, Point> map = new HashMap<>(points.size());
        for (int i = 0; i < ids.size() && i < points.size(); i++) {
            if (points.get(i) != null) {
                map.put(ids.get(i), points.get(i));
            }
        }
        return map;
    }

    private long idleMillis(LocalDateTime updateTime, long now) {

        if (updateTime == null) {
            return 0L;
        }
        return Math.max(0L, now - updateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
}