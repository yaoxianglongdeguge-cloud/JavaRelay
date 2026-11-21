package com.example.javarelay.strategysapi;

import com.example.javarelay.dto.Result;
import com.example.javarelay.strategysimpl.infocollect.InfoCollector;

public interface SelectPathStrategy {

    public String getType();

    public Result findPathStrategy(InfoCollector infoCollector);
}
