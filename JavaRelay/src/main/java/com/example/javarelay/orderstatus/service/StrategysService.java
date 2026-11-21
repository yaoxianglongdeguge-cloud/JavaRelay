package com.example.javarelay.orderstatus.service;

import com.example.javarelay.dto.Result;
import com.example.javarelay.strategysimpl.infocollect.InfoCollector;

public interface StrategysService {

    public Result putInfoCollector(Long orderId);

    public Result deleteInfoCollector(Long orderId);

    public InfoCollector getInfoCollector(Long orderId);

    public Result findPassengerByStrategy(Long orderId);

    public Result findDriverByStrategy(Long orderId);

    public Result findPathByStrategy(Long orderId);

    public Result preComputeMoney(Long orderId);

    public Result ComputeMoney(Long orderId);

}
