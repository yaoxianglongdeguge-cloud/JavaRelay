package com.example.javarelay.statustransition.service;

import com.example.javarelay.statustransition.dto.DriverDto;
import com.example.javarelay.dto.Result;

public interface DriverOperateService {

    public Result CancelOrder(Long driverId);

    public Result FinishOrder(Long driverId);

    public Result ReceiveOrder(DriverDto driverDto);

    public Result ArriveOrder(Long driverId);

}
