package com.example.javarelay.strategys;

import com.example.javarelay.dto.DriverDto;
import com.example.javarelay.dto.Result;
import com.example.javarelay.infocollect.InfoCollector;

import java.util.List;

public interface SelectDriverStrategy {
    public Result findDriverStrategy(InfoCollector infoCollector);
}
