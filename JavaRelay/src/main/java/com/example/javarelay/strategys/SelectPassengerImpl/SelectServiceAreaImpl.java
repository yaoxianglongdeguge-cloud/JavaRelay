package com.example.javarelay.strategys.SelectPassengerImpl;

import com.example.javarelay.constant.RedisPefix;
import com.example.javarelay.dto.Result;
import com.example.javarelay.infra.GeoUtil;
import com.example.javarelay.infocollect.InfoCollector;
import com.example.javarelay.strategys.SelectPassengerStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.apache.ibatis.ognl.OgnlOps.doubleValue;

public class SelectServiceAreaImpl implements SelectPassengerStrategy {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Override
    public Result findPassengerStrategy(InfoCollector infoCollector) {
        return inServiceArea(infoCollector);
    }

    public Result inServiceArea(InfoCollector infoCollector){

        Long passengerId = infoCollector.getPassengerInfo().getPassengerId();
        BigDecimal startLng = infoCollector.getPassengerInfo().getStartLng();
        BigDecimal startLat = infoCollector.getPassengerInfo().getStartLat();

        //确定是否在服务范围内
        String city = infoCollector.getPassengerInfo().getCity();

        Map<Object,Object> siteCity = redisTemplate.opsForHash().
                entries(RedisPefix.SERVICEAREA_PREFIX+city);

        Map<String, Double> result = new HashMap<>();
        siteCity.forEach((k, v) ->
                result.put(String.valueOf(k), (Double)v)
        );
        double distance = GeoUtil.getDistance(doubleValue(startLng),doubleValue(startLat),
                result.get("centerLng"),result.get("centerLat"));

        if(distance > result.get("centerRedius")){
            return Result.fail("不在服务范围内");
        }

        return Result.success();
    }
}
