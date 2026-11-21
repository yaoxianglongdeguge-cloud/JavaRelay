package com.example.javarelay.orderstatus.pojo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RouteInfo {
    private BigDecimal startLng;
    private BigDecimal startLat;
    private String startAddress;

    private BigDecimal endLng;
    private BigDecimal endLat;
    private String endAddress;

    private BigDecimal distanceKm;
    private Integer durationMinutes;
    private String polyline;

    public boolean setStartAndEnd(
            BigDecimal startLng,
            BigDecimal startLat,
            BigDecimal endLng,
            BigDecimal endLat){

        this.setStartLat(startLat);
        this.setStartLng(startLng);
        this.setEndLat(endLat);
        this.setEndLng(endLng);

        return true;
    }
}