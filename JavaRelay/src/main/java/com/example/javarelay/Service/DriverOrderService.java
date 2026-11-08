package com.example.javarelay.Service;

import com.example.javarelay.Dto.DriverDto;
import com.example.javarelay.Dto.PassengerDto;
import com.example.javarelay.Dto.Result;

public interface DriverOrderService {

    public Result CancelOrder(DriverDto driverDto);

    public Result FinishOrder(DriverDto driverDto);

    public Result ReceiverOrder(DriverDto driverDto);

}
