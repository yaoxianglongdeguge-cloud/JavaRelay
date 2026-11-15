package com.example.javarelay.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

@Data
public class PassengerDto {

    Long passengerId;

    private String strategy;

    private String precomputeMoneyType;

    private String computeMoneyType;

    private BigDecimal startLng;
    private BigDecimal startLat;

    private BigDecimal endLng;
    private BigDecimal endLat;

}
