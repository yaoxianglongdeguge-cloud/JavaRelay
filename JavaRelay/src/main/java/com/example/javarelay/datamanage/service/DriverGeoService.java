package com.example.javarelay.datamanage.service;

import com.example.javarelay.dto.Result;

import java.math.BigDecimal;


public interface DriverGeoService {

    Result report(Long driverId, BigDecimal lng, BigDecimal lat);

    Result nearby(BigDecimal lng, BigDecimal lat, double radiusMeters, int limit);

    Result isFresh(Long driverId);

    Result remove(Long driverId);
}