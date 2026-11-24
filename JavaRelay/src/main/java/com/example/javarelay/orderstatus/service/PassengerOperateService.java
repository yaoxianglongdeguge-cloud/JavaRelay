package com.example.javarelay.orderstatus.service;

import com.example.javarelay.orderstatus.dto.PassengerDto;
import com.example.javarelay.dto.Result;

public interface PassengerOperateService {

    public Result PutOrder(PassengerDto passengerDto);

    public Result ReDispatchOrder(Long orderId,Long passengerId);

    public Result AssignPassenger(Long orderId,Long passengerId);

    public Result AssignDriver(Long orderId,Long passengerId);

    public Result AssignPath(Long orderId,Long passengerId);

    public Result CancelOrder(Long passengerId);
    public Result FinishOrder(Long passengerId);

    public Result ReComputeMoney(Long passengerId);

}
