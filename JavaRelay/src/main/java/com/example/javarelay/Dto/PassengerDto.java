package com.example.javarelay.Dto;

import com.example.javarelay.Entity.Passenger;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Set;

@Data
public class PassengerDto {

    Long passengerId;

    public BigDecimal startLng;
    public BigDecimal startLat;
    public BigDecimal endLng;
    public BigDecimal endLat;

    public Set<Integer> model;

    public String strategy;

}
