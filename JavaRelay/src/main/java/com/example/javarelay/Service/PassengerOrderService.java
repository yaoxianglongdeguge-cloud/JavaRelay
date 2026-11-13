package com.example.javarelay.service;

import com.example.javarelay.dto.PassengerDto;
import com.example.javarelay.dto.Result;
import com.example.javarelay.entity.Passenger;

public interface PassengerOrderService {

    public Result PutOrder(PassengerDto passengerDto);

    public Result ReDispatchOrder(PassengerDto passengerDto);

    public Result CancelOrder(PassengerDto passengerDto);
    public Result FinishOrder(PassengerDto passengerDto);

}
