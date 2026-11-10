package com.example.javarelay.service;

import com.example.javarelay.dto.Result;

public interface StrategysService {

    public Result findDriverByStrategy(String type,Long orderId);

    public Result findPathByStrategy(String type,Long orderId);

    public Result preComputeMoney(String type,Long orderId);

    public Result ComputeMoney(String type,Long orderId);

}
