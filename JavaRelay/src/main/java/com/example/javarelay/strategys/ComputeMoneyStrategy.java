package com.example.javarelay.strategys;

import com.example.javarelay.dto.Result;
import com.example.javarelay.infocollect.InfoCollector;

public interface ComputeMoneyStrategy {

    public Result ComputeStrategy(InfoCollector infoCollector);

}
