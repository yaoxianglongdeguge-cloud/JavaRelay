package com.example.javarelay.orderstatus.backgroundtask;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.data.mapper.DriverDetailMapper;
import com.example.javarelay.infra.redis.RedisUtil;
import com.example.javarelay.infra.redis.rediscommon.RedisPefix;
import com.example.javarelay.orderstatus.constants.Constants;
import com.example.javarelay.pojo.DriverAttr;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 司机"慢变属性"巡检。
 *
 * <p>与 {@link DriverPatrolTask} 的分工：
 * <ul>
 *   <li>DriverPatrolTask：状态（忙/闲），3 秒一轮</li>
 *   <li>DriverAttrPatrolTask：属性（评分等），60 秒一轮</li>
 * </ul>
 * 分开的原因是两者变化频率差两个数量级，混在一起只会造成写放大。
 *
 * <p>写 {@code service:driver:attr:{id}}，TTL {@link #TTL_SECONDS} 秒，
 * 超过 3 倍刷新间隔自动过期，避免司机被删后留下脏数据。
 */
@Slf4j
@Component
public class DriverAttrPatrolTask {

    /** 刷新间隔 */
    private static final long INTERVAL_MS = 60_000L;

    /** 缓存 TTL：3 倍冗余 */
    private static final long TTL_SECONDS = 180L;

    private static final AtomicInteger nextScan = new AtomicInteger(0);

    @Autowired
    DriverDetailMapper driverDetailMapper;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    RedisUtil redisUtil;

    @Scheduled(fixedDelay = INTERVAL_MS)
    private void scanAttr() {

        String token = UUID.randomUUID().toString();

        if (!redisUtil.tryLock(RedisPefix.DRIVER_ATTR_LOCK_KEY, token,
                RedisPefix.DRIVER_ATTR_LEADER_TTL)) {
            return;
        }

        List<DriverAttr> attrs;

        try {
            attrs = driverDetailMapper.getDriverAttrs(nextScan.get(), Constants.SERVE_DRIVERS_SCAN);
        } catch (Exception e) {
            log.warn("司机属性扫描出错");
            return;
        }

        if (attrs == null) {
            nextScan.set(0);
            return;
        }

        // 游标推进：扫满一批就往后挪，否则回绕
        if (attrs.size() < Constants.SERVE_DRIVERS_SCAN) {
            nextScan.set(0);
        } else {
            nextScan.addAndGet(attrs.size());
        }

        if (attrs.isEmpty()) {
            return;
        }

        int written = 0;
        int missing = 0;

        for (DriverAttr attr : attrs) {

            if (attr == null || attr.getId() == null) {
                continue;
            }

            // 右连接保证有 detail 记录；score 为 NULL 说明数据异常（列可空）
            if (attr.getScore() == null) {
                missing++;
                continue;
            }

            try {
                stringRedisTemplate.opsForValue().set(
                        RedisPefix.DRIVER_ATTR_PREFIX + attr.getId(),
                        JSON.toJSONString(attr),
                        Duration.ofSeconds(TTL_SECONDS));
                written++;
            } catch (Exception e) {
                log.error("写司机属性缓存失败 driverId={}", attr.getId(), e);
            }
        }

        if (missing > 0) {
            log.warn("司机属性巡检：本批 {} 条中有 {} 条缺少 driver_detail 记录（数据不一致）",
                    attrs.size(), missing);
        }

        log.info("司机属性巡检：扫描 {} 条，写入 {} 条", attrs.size(), written);
    }
}