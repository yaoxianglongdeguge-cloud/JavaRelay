package com.example.javarelay.strategysimpl.SelectPathImpl;

import com.example.javarelay.dto.Result;
import com.example.javarelay.statustransition.infocollect.InfoCollector;
import com.example.javarelay.statustransition.strategys.SelectPathStrategy;
import org.springframework.stereotype.Component;

@Component
public class SelectPathImpl implements SelectPathStrategy {

    String type = "default";

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Result findPathStrategy(InfoCollector infoCollector) {
        return null;
    }
}
