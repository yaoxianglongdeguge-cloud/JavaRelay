package com.example.javarelay.service.Impl;

import com.example.javarelay.dto.PassengerDto;
import com.example.javarelay.dto.Result;
import com.example.javarelay.entity.Passenger;
import com.example.javarelay.infocollect.InfoCollector;
import com.example.javarelay.strategys.*;
import com.example.javarelay.service.StrategysService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class StrategysServiceImpl implements StrategysService {

    @Autowired
    Map<Long,InfoCollector> infoCollectors;

    Map<String, SelectDriverStrategy> selectDriverStrategys;

    Map<String, SelectPassengerStrategy> selectPassengerStrategys;

    Map<String, SelectPathStrategy> selectPathStrategys;

    Map<String, PreComputeStrategy> preComputeStrategys;

    Map<String, ComputeMoneyStrategy> computeMoneyStrategys;



    public StrategysServiceImpl(Map<String, SelectDriverStrategy> SelectDriverStrategys){
        this.selectDriverStrategys = SelectDriverStrategys;
    }

    public Result putInfoCollector(Long orderId, PassengerDto passengerDto){
        return null;
    }

    @Override
    public Result findPassengerByStrategy(Long orderId) {
        InfoCollector infoCollector = infoCollectors.get(orderId);
        String type = infoCollector.getSelectPassengerStrategy();
        return selectPassengerStrategys.get(type).findPassengerStrategy(infoCollector);
    }

    @Override
    public Result findDriverByStrategy(Long orderId) {

        InfoCollector infoCollector = infoCollectors.get(orderId);
        String type = infoCollector.getSelectDriverStrategy();
        return selectDriverStrategys.get(type).findDriverStrategy(infoCollector);
    }

    @Override
    public Result findPathByStrategy(Long orderId) {
        return null;
    }

    @Override
    public Result preComputeMoney(Long orderId) {
        return null;
    }

    @Override
    public Result ComputeMoney(Long orderId) {
        return null;
    }

}
