package com.example.javarelay.strategysapi;

import com.example.javarelay.dto.Result;
import com.example.javarelay.strategysimpl.infocollect.InfoCollector;

public interface SelectPassengerStrategy {

    public String getType();

    public Result findPassengerStrategy(InfoCollector infoCollector);
}
