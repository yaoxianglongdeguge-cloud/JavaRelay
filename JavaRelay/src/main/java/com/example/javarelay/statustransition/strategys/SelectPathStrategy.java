package com.example.javarelay.statustransition.strategys;

import com.example.javarelay.dto.Result;
import com.example.javarelay.statustransition.infocollect.InfoCollector;

public interface SelectPathStrategy {

    public String getType();

    public Result findPathStrategy(InfoCollector infoCollector);
}
