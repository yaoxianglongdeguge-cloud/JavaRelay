package com.example.javarelay.datamanage.service.impl;

import com.example.javarelay.datamanage.service.DriverGeoService;
import com.example.javarelay.dto.Result;
import com.example.javarelay.infra.redis.rediscommon.RedisPefix;
import com.example.javarelay.pojo.DriverGeo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DriverGeoServiceImpl implements DriverGeoService {

    /** 位置新鲜度 TTL：超过这个时间没上报，视为位置不可信 */
    public static final long ALIVE_TTL_SECONDS = 60L;

    private static final double MIN_LNG = -180D;
    private static final double MAX_LNG = 180D;
    private static final double MIN_LAT = -90D;
    private static final double MAX_LAT = 90D;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Override
    public Result report(Long driverId, BigDecimal lng, BigDecimal lat) {

        if (driverId == null) {
            return Result.fail("司机未登录");
        }
        if (lng == null || lat == null) {
            return Result.fail("位置信息不全");
        }

        double x = lng.doubleValue();
        double y = lat.doubleValue();

        if (!isValidLng(x) || !isValidLat(y)) {
            log.warn("司机位置参数非法 driverId={} lng={} lat={}", driverId, x, y);
            return Result.fail("位置参数非法");
        }

        try {
            redisTemplate.opsForGeo().add(
                    RedisPefix.DRIVER_GEO,
                    new Point(x, y),
                    String.valueOf(driverId));

            // 带 TTL 的心跳，用来判断位置新鲜度
            redisTemplate.opsForValue().set(
                    RedisPefix.DRIVER_LOC_ALIVE_PREFIX + driverId,
                    String.valueOf(System.currentTimeMillis()),
                    Duration.ofSeconds(ALIVE_TTL_SECONDS));

            return Result.success("位置已更新");

        } catch (Exception e) {
            log.error("司机位置上报失败 driverId={}", driverId, e);
            return Result.fail("位置写入失败");
        }
    }

    @Override
    public Result nearby(BigDecimal lng, BigDecimal lat, double radiusMeters, int limit) {

        List<DriverGeo> list = new ArrayList<>();

        if (lng == null || lat == null || radiusMeters <= 0 || limit <= 0) {
            return Result.fail("查询参数非法");
        }

        try {
            GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo().search(
                    RedisPefix.DRIVER_GEO,
                    GeoReference.fromCoordinate(lng.doubleValue(), lat.doubleValue()),

                    new Distance(radiusMeters, Metrics.NEUTRAL),
                    RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                            .includeDistance()
                            .sortAscending()
                            .limit(limit));

            if (results != null) {
                for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results) {

                    Long driverId = parseLong(result.getContent().getName());

                    if (driverId == null) {
                        continue;
                    }

                    DriverGeo geo = new DriverGeo();
                    geo.setDriverId(driverId);
                    geo.setDistanceMeters(result.getDistance() == null
                            ? 0D : result.getDistance().getValue());

                    list.add(geo);
                }
            }

        } catch (Exception e) {
            log.error("附近司机查询失败 lng={} lat={}", lng, lat, e);
            return Result.fail("附近司机查询失败");
        }

        return Result.success("查询成功", list);
    }

    @Override
    public Result isFresh(Long driverId) {

        if (driverId == null) {
            return Result.fail("司机未登录");
        }

        try {
            boolean fresh = Boolean.TRUE.equals(redisTemplate.hasKey(
                    RedisPefix.DRIVER_LOC_ALIVE_PREFIX + driverId));
            return Result.success("查询成功", fresh);
        } catch (Exception e) {
            log.error("查询位置新鲜度失败 driverId={}", driverId, e);
            return Result.fail("查询失败");
        }
    }

    @Override
    public Result remove(Long driverId) {

        if (driverId == null) {
            return Result.fail("司机未登录");
        }

        try {
            redisTemplate.opsForGeo().remove(RedisPefix.DRIVER_GEO, String.valueOf(driverId));
            redisTemplate.delete(RedisPefix.DRIVER_LOC_ALIVE_PREFIX + driverId);
            return Result.success("位置已移除");
        } catch (Exception e) {
            log.error("移除司机位置失败 driverId={}", driverId, e);
            return Result.fail("移除失败");
        }
    }

    private static boolean isValidLng(double lng) {
        return lng >= MIN_LNG && lng <= MAX_LNG;
    }

    private static boolean isValidLat(double lat) {
        return lat >= MIN_LAT && lat <= MAX_LAT;
    }

    private static Long parseLong(String s) {
        try {
            return Long.valueOf(s);
        } catch (Exception e) {
            return null;
        }
    }
}