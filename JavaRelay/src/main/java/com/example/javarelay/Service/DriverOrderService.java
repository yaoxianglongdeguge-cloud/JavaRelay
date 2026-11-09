package com.example.javarelay.service;

import com.example.javarelay.dto.DriverDto;
import com.example.javarelay.dto.Result;

public interface DriverOrderService {

    public Result CancelOrder(DriverDto driverDto);

    public Result FinishOrder(DriverDto driverDto);

    public Result ReceiverOrder(DriverDto driverDto);

}
