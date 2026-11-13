package com.example.javarelay.strategys.SelectDriversImpl;

import com.example.javarelay.dto.DriverDto;
import com.example.javarelay.dto.Result;
import com.example.javarelay.infocollect.InfoCollector;
import com.example.javarelay.strategys.SelectDriverStrategy;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DefaultDriverStrategy implements SelectDriverStrategy {
    @Override
    public Result findDriverStrategy(InfoCollector infoCollector) {
        return null;
    }
}
