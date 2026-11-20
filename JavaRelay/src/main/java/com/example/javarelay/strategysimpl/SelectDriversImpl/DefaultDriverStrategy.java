package com.example.javarelay.strategysimpl.SelectDriversImpl;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.data.entity.DriverRedis;
import com.example.javarelay.data.mapper.DriverMapper;
import com.example.javarelay.dto.Result;
import com.example.javarelay.rediscommon.RedisPefix;
import com.example.javarelay.statustransition.dto.DriverDto;
import com.example.javarelay.statustransition.infocollect.InfoCollector;
import com.example.javarelay.statustransition.statusenum.DriverStatusEnum;
import com.example.javarelay.statustransition.strategys.SelectDriverStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class DefaultDriverStrategy implements SelectDriverStrategy {

    /** 本地池子里少于这个数就去 Redis 补货 */
    private static final int REFILL_THRESHOLD = 10;
    /** 一次最多缓存多少个候选 */
    private static final int MAX_BATCH = 200;
    /** 兜底刷新间隔：司机释放回池不会主动通知，靠这个把新释放的司机捞回来 */
    private static final long REFRESH_INTERVAL_MS = 2000;

    private final String type = "default";

    @Autowired
    private StringRedisTemplate redisTemplate;

    /** 本地候选池（整体替换，不原地 clear，避免并发读到空） */
    private final AtomicReference<Queue<Long>> pool =
            new AtomicReference<>(new ConcurrentLinkedQueue<>());

    /** 保证同一时刻只有一个线程去 Redis 补货 */
    private final AtomicBoolean refilling = new AtomicBoolean(false);

    private volatile long lastRefresh = 0L;

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Result findDriverStrategy(InfoCollector infoCollector) {
        Queue<Long> current = pool.get();
        Long candidate = current.poll();

        boolean needRefill = candidate == null
                || current.size() < REFILL_THRESHOLD
                || System.currentTimeMillis() - lastRefresh > REFRESH_INTERVAL_MS;

        if (needRefill) {
            refill();
            candidate = pool.get().poll();      // 补完再取一次
        }

        if (candidate == null) {
            return Result.fail("附近没有空闲司机");
        }

        DriverDto dto = new DriverDto();
        dto.setDriverId(candidate);
        return Result.success(dto);
    }

    private void refill() {
        if (!refilling.compareAndSet(false, true)) {
            return;                              // 别的线程正在补，直接用现有池子
        }
        try {
            Set<String> keys = redisTemplate.keys(RedisPefix.DRIVER_INFO + "*");
            if (keys == null || keys.isEmpty()) {
                return;
            }

            List<String> values = redisTemplate.opsForValue().multiGet(keys);
            List<Long> idle = new ArrayList<>();
            if (values != null) {
                for (String json : values) {
                    if (json == null) continue;
                    DriverRedis d = JSON.parseObject(json, DriverRedis.class);
                    if (d != null && d.getStatus() != null
                            && d.getStatus() == DriverStatusEnum.LEISURE.getCode()) {
                        idle.add(d.getId());
                    }
                }
            }

            Collections.shuffle(idle);           // 打散，避免总是抢同几个司机
            List<Long> batch = idle.subList(0, Math.min(idle.size(), MAX_BATCH));

            // 整体换掉，不 clear 原队列（避免并发线程读到空池）
            pool.set(new ConcurrentLinkedQueue<>(batch));
            lastRefresh = System.currentTimeMillis();

        } catch (Exception e) {
            // 补货失败不影响主流程，继续用旧池子
        } finally {
            refilling.set(false);
        }
    }
}