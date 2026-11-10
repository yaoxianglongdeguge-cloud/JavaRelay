package com.example.javarelay.service.Impl;

import com.example.javarelay.dto.Result;
import com.example.javarelay.strategys.InfoCollector;
import com.example.javarelay.strategys.SelectDriverStrategy;
import com.example.javarelay.service.StrategysService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class StrategysServiceImpl implements StrategysService {

    @Autowired
    InfoCollector infoCollector;

    Map<String, SelectDriverStrategy> SelectDriverStrategys;

    public StrategysServiceImpl(Map<String, SelectDriverStrategy> SelectDriverStrategys){
        this.SelectDriverStrategys = SelectDriverStrategys;
    }

    @Override
    public Result findDriverByStrategy(String type,Long orderId) {

        SelectDriverStrategys.get(type).findByStrategy();

        return null;
    }

    @Override
    public Result findPathByStrategy(String type,Long orderId) {
        return null;
    }

    @Override
    public Result preComputeMoney(String type, Long orderId) {
        return null;
    }

    @Override
    public Result ComputeMoney(String type, Long orderId) {
        return null;
    }
}
