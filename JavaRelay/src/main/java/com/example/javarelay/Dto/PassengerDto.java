package com.example.javarelay.dto;

import com.example.javarelay.entity.Passenger;
import com.example.javarelay.infocollect.InfoCollector;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

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
