package com.example.javarelay.strategys;

import com.example.javarelay.dto.Result;
import com.example.javarelay.infocollect.InfoCollector;

public interface PreComputeStrategy {

    public String getType();

    public Result PreComputeStrategy(InfoCollector infoCollector);
}
