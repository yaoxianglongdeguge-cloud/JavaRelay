package com.example.javarelay.strategysimpl.strategysimpl.SelectPathImpl;

import com.example.javarelay.dto.Result;
import com.example.javarelay.infra.GeoUtil;
import com.example.javarelay.orderstatus.pojo.RouteInfo;
import com.example.javarelay.strategysimpl.infocollect.InfoCollector;
import com.example.javarelay.strategysapi.SelectPathStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class SelectPathImpl implements SelectPathStrategy {

    String type = "default";

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Result findPathStrategy(InfoCollector infoCollector) {

        RouteInfo route = new RouteInfo();
        route.setStartAndEnd(
                infoCollector.getStartLng(), infoCollector.getStartLat(),
                infoCollector.getEndLng(), infoCollector.getEndLat());
        route.setStartAddress(infoCollector.getStartAddress());
        route.setEndAddress(infoCollector.getEndAddress());

        double meters = GeoUtil.getDistance(
                infoCollector.getStartLng().doubleValue(), infoCollector.getStartLat().doubleValue(),
                infoCollector.getEndLng().doubleValue(), infoCollector.getEndLat().doubleValue());
        route.setDistanceKm(BigDecimal.valueOf(meters / 1000.0)
                .setScale(2, RoundingMode.HALF_UP));

        return Result.success(route);
    }
}
