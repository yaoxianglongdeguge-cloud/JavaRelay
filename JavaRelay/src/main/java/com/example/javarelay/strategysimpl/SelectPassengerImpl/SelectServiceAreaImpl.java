package com.example.javarelay.strategysimpl.SelectPassengerImpl;

import com.example.javarelay.data.entity.ServiceArea;
import com.example.javarelay.dto.Result;
import com.example.javarelay.infra.GeoUtil;
import com.example.javarelay.statustransition.infocollect.InfoCollector;
import com.example.javarelay.statustransition.service.ServeAreaService;
import com.example.javarelay.statustransition.strategys.SelectPassengerStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;


import java.math.BigDecimal;

@Component
public class SelectServiceAreaImpl implements SelectPassengerStrategy {

    String type = "default";

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ServeAreaService serveAreaService;

    public String getType(){
        return this.type;
    }

    @Override
    public Result findPassengerStrategy(InfoCollector infoCollector) {
        return inServiceArea(infoCollector);
    }

    public Result inServiceArea(InfoCollector infoCollector){

        Long passengerId = infoCollector.getPassengerId();
        BigDecimal startLng = infoCollector.getStartLng();
        BigDecimal startLat = infoCollector.getStartLat();

        //确定是否在服务范围内
        String city = infoCollector.getStartAddress();


        ServiceArea serviceArea = serveAreaService.getServiceAreaRedis(city);
        if(serviceArea == null){
            return Result.fail("不在服务范围内");
        }

        double distance = GeoUtil.getDistance(
                startLng.doubleValue(), startLat.doubleValue(),
                serviceArea.getCenterLng().doubleValue(), serviceArea.getCenterLat().doubleValue());


        if(distance > serviceArea.getRadius()){
            return Result.fail("不在服务范围内");
        }

        return Result.success();
    }
}
