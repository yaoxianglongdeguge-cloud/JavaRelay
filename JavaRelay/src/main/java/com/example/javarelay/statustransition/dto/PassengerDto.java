package com.example.javarelay.statustransition.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PassengerDto {

    Long passengerId;

    private String selectDriverStrategy;

    private String computeMoneyType;

    private BigDecimal startLng;
    private BigDecimal startLat;
    private String startAddress;

    private BigDecimal endLng;
    private BigDecimal endLat;
    private String endAddress;

    public PassengerDto(){

    }


}
