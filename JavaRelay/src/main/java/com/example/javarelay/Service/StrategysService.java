package com.example.javarelay.service;

import com.example.javarelay.dto.PassengerDto;
import com.example.javarelay.dto.Result;

public interface StrategysService {

    public Result putInfoCollector(Long orderId, PassengerDto passengerDto);

    public Result findPassengerByStrategy(Long orderId);

    public Result findDriverByStrategy(Long orderId);

    public Result findPathByStrategy(Long orderId);

    public Result preComputeMoney(Long orderId);

    public Result ComputeMoney(Long orderId);

}
