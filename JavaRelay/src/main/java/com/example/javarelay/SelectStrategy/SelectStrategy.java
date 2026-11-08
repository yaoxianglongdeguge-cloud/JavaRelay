package com.example.javarelay.SelectStrategy;

import com.example.javarelay.Dto.DriverDto;

import java.util.List;

public interface SelectStrategy {

    public List<DriverDto> findByStrategy();
}
