package com.example.javarelay.statustransition.service;

import com.example.javarelay.statustransition.dto.PassengerDto;
import com.example.javarelay.dto.Result;

public interface PassengerOperateService {

    public Result PutOrder(PassengerDto passengerDto);

    public Result ReDispatchOrder(Long orderId,Long passengerId);

    public Result CancelOrder(Long passengerId);
    public Result FinishOrder(Long passengerId);

    public Result ReComputeMoney(Long passengerId);

}
