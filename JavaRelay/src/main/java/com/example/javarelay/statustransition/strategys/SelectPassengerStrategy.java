package com.example.javarelay.statustransition.strategys;

import com.example.javarelay.dto.Result;
import com.example.javarelay.statustransition.infocollect.InfoCollector;

public interface SelectPassengerStrategy {

    public String getType();

    public Result findPassengerStrategy(InfoCollector infoCollector);
}
