package com.example.javarelay.orderstatus.dispatch;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.example.javarelay.datamanage.service.DriverGeoService;
import com.example.javarelay.dto.Result;
import com.example.javarelay.infra.redis.rediscommon.RedisPefix;
import com.example.javarelay.pojo.DriverCandidate;
import com.example.javarelay.strategysimpl.infocollect.InfoCollector;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 候选司机提供者：把一个订单变成"半径内可用司机 + 距离 + 状态 + 评分"。
 *
 * <p>三步：
 * <ol>
 *   <li>GEO 半径查询（拿司机和直线距离）</li>
 *   <li>批量读状态（{@code service:driver:info}），状态缺失的司机视为不可用</li>
 *   <li>批量读属性（{@code service:driver:attr}），缺失用默认评分并打点</li>
 * </ol>
 */
@Slf4j
@Component
public class DriverCandidateProvider {

    /** 司机状态缓存 */
    private static final String INFO_PREFIX = RedisPefix.DRIVER_INFO;

    /** 司机属性缓存 */
    private static final String ATTR_PREFIX = RedisPefix.DRIVER_ATTR_PREFIX;

    @Autowired
    DriverGeoService driverGeoService;

    @Autowired
    StringRedisTemplate redisTemplate;

    /** 取一个订单的候选司机（按距离升序，状态/属性已补全） */
    public OrderCandidates candidatesOf(DispatchJob job) {

        OrderCandidates result = new OrderCandidates(job.getOrderId());

        InfoCollector ctx = job.getCtx();

        if (ctx == null || ctx.getStartLng() == null || ctx.getStartLat() == null) {
            return result;
        }

        Result nearby = driverGeoService.nearby(
                ctx.getStartLng(), ctx.getStartLat(),
                DispatchConstants.RADIUS_METERS, DispatchConstants.NEARBY_LIMIT);

        if (nearby == null || nearby.getCode() != 200 || !(nearby.getData() instanceof List)) {
            return result;
        }

        @SuppressWarnings("unchecked")
        List<DriverCandidate> near = (List<DriverCandidate>) nearby.getData();

        for (DriverCandidate candidate : near) {
            if (candidate != null && candidate.getId() != null) {
                result.put(candidate);
            }
        }

        enrich(result);

        return result;
    }

    /** 批量补状态与评分 */
    private void enrich(OrderCandidates candidates) {

        List<Long> ids = new ArrayList<>(candidates.driverIds());

        if (ids.isEmpty()) {
            return;
        }

        List<String> infoKeys = new ArrayList<>(ids.size());
        List<String> attrKeys = new ArrayList<>(ids.size());

        for (Long id : ids) {
            infoKeys.add(INFO_PREFIX + id);
            attrKeys.add(ATTR_PREFIX + id);
        }

        List<String> infos;
        List<String> attrs;

        try {
            infos = redisTemplate.opsForValue().multiGet(infoKeys);
            attrs = redisTemplate.opsForValue().multiGet(attrKeys);
        } catch (Exception e) {
            log.error("读取司机状态/属性失败", e);
            return;
        }

        for (int i = 0; i < ids.size(); i++) {

            DriverCandidate candidate = candidates.get(ids.get(i));

            if (candidate == null) {
                continue;
            }

            // 状态缺失 → status 保持 null → isIdle() 为 false → 不参与匹配
            String infoJson = infos == null || i >= infos.size() ? null : infos.get(i);

            if (infoJson != null) {
                JSONObject info = JSON.parseObject(infoJson);
                if (info != null) {
                    candidate.setStatus(info.getInteger("status"));
                }
            }

            String attrJson = attrs == null || i >= attrs.size() ? null : attrs.get(i);

            Double score = null;

            if (attrJson != null) {
                JSONObject attr = JSON.parseObject(attrJson);
                if (attr != null) {
                    score = attr.getDouble("score");
                }
            }

            candidate.setAttrMissing(score == null);
            candidate.setScore(score == null ? DispatchConstants.DEFAULT_SCORE : score);
        }
    }

    /** 一个订单的候选集合：driverId -> 候选（保持 GEO 返回的距离升序） */
    @Data
    public static class OrderCandidates {

        private final Long orderId;

        private final Map<Long, DriverCandidate> byDriver = new LinkedHashMap<>();

        public OrderCandidates(Long orderId) {
            this.orderId = orderId;
        }

        public void put(DriverCandidate candidate) {
            byDriver.put(candidate.getId(), candidate);
        }

        public DriverCandidate get(Long driverId) {
            return byDriver.get(driverId);
        }

        public Collection<DriverCandidate> all() {
            return byDriver.values();
        }

        public Collection<Long> driverIds() {
            return byDriver.keySet();
        }

        public int size() {
            return byDriver.size();
        }

        public boolean isEmpty() {
            return byDriver.isEmpty();
        }
    }
}