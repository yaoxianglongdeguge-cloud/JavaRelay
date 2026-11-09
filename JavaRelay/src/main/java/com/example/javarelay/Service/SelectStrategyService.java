package com.example.javarelay.service;

import com.example.javarelay.dto.DriverDto;

import java.util.List;

public interface SelectStrategyService {

    public List<DriverDto> findByStrategy(String type);

}
