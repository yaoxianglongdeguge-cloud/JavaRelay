package com.example.javarelay.Service;

import com.example.javarelay.Dto.DriverDto;

import java.util.List;

public interface SelectStrategyService {

    public List<DriverDto> findByStrategy(String type);

}
