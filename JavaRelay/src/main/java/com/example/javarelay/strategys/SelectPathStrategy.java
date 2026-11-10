package com.example.javarelay.strategys;

import com.example.javarelay.dto.PathInfoDto;
import com.example.javarelay.dto.Result;
import com.example.javarelay.infocollect.InfoCollector;

public interface SelectPathStrategy {

    public Result findPathStrategy(InfoCollector infoCollector);
}
