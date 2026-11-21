package com.example.javarelay.orderstatus.service;

import com.example.javarelay.orderstatus.dto.DriverDto;
import com.example.javarelay.dto.Result;

public interface DriverOperateService {

    public Result CancelOrder(Long driverId);

    public Result FinishOrder(Long driverId);

    public Result ReceiveOrder(DriverDto driverDto);

    public Result ArriveOrder(Long driverId);

}
