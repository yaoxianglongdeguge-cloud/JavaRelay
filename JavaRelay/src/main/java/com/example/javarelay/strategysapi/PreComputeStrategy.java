package com.example.javarelay.strategysapi;

import com.example.javarelay.dto.Result;
import com.example.javarelay.strategysimpl.infocollect.InfoCollector;

public interface PreComputeStrategy {

    public String getType();

    public Result PreComputeStrategy(InfoCollector infoCollector);
}
