package com.example.javarelay.datamanage.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 司机位置上报体。
 */
@Data
public class DriverGeoDto {

    private BigDecimal lng;

    private BigDecimal lat;
}