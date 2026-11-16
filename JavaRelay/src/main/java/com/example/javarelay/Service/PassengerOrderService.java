package com.example.javarelay.service;

import com.example.javarelay.dto.PassengerDto;
import com.example.javarelay.dto.Result;
import com.example.javarelay.entity.Passenger;

public interface PassengerOrderService {

    public Result PutOrder(PassengerDto passengerDto);

    public Result ReDispatchOrder(Long orderId,Long passengerId);

    public Result CancelOrder(PassengerDto passengerDto);
    public Result FinishOrder(PassengerDto passengerDto);

    public Result ReComputeMoney(PassengerDto passengerDto, Long orderId);

}
