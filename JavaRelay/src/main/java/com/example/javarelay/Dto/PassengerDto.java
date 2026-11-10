package com.example.javarelay.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

@Data
public class PassengerDto {

    Long passengerId;

    private BigDecimal startLng;
    private BigDecimal startLat;
    private BigDecimal endLng;
    private BigDecimal endLat;
    private String city;

    private String strategy;
    private Integer strategyMark;

    private String precomputeMoneyType;
    private Integer precomMark;

    private String computeMoneyType;
    private Integer computeMark;

}
