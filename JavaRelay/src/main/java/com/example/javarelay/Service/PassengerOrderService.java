package com.example.javarelay.service;

import com.example.javarelay.dto.PassengerDto;
import com.example.javarelay.dto.Result;

public interface PassengerOrderService {

    public Result PutOrder(PassengerDto passengerDto);

    public Result CancelOrder(PassengerDto passengerDto);
    public Result FinishOrder(PassengerDto passengerDto);

}
