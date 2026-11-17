package com.example.javarelay.statustransition.strategys;

import com.example.javarelay.dto.Result;
import com.example.javarelay.statustransition.infocollect.InfoCollector;

public interface PreComputeStrategy {

    public String getType();

    public Result PreComputeStrategy(InfoCollector infoCollector);
}
