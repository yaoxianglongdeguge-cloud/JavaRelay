package com.example.javarelay.strategysimpl.SelectDriversImpl;

import com.example.javarelay.dto.Result;
import com.example.javarelay.statustransition.infocollect.InfoCollector;
import com.example.javarelay.statustransition.strategys.SelectDriverStrategy;
import org.springframework.stereotype.Component;

@Component
public class DefaultDriverStrategy implements SelectDriverStrategy {

    String type = "default";

    public String getType(){
        return this.type;
    }

    @Override
    public Result findDriverStrategy(InfoCollector infoCollector) {
        return null;
    }
}
