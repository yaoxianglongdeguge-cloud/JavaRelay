package com.example.javarelay.Dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Set;

@Data
public class PassengerDto {

    Integer passengerId;

    public BigDecimal startLng;
    public BigDecimal startLat;

    public Set<Integer> model;

    public String strategy;
}
