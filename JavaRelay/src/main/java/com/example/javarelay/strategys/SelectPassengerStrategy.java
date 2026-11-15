package com.example.javarelay.strategys;

import com.example.javarelay.dto.Result;
import com.example.javarelay.infocollect.InfoCollector;

public interface SelectPassengerStrategy {

    public String getType();

    public Result findPassengerStrategy(InfoCollector infoCollector);
}
