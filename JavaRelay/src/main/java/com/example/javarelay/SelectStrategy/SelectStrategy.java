package com.example.javarelay.selectstrategy;

import com.example.javarelay.dto.DriverDto;

import java.util.List;

public interface SelectStrategy {

    public List<DriverDto> findByStrategy();
}
