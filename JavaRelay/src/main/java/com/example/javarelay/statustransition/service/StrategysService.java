package com.example.javarelay.statustransition.service;

import com.example.javarelay.statustransition.dto.PassengerDto;
import com.example.javarelay.dto.Result;
import com.example.javarelay.statustransition.infocollect.InfoCollector;

public interface StrategysService {

    public Result putInfoCollector(Long orderId, PassengerDto passengerDto);

    public Result deleteInfoCollector(Long orderId);

    public InfoCollector getInfoCollector(Long orderId);

    public Result findPassengerByStrategy(Long orderId);

    public Result findDriverByStrategy(Long orderId);

    public Result findPathByStrategy(Long orderId);

    public Result preComputeMoney(Long orderId);

    public Result ComputeMoney(Long orderId);

}
