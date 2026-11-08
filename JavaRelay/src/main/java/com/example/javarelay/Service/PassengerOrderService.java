package com.example.javarelay.Service;

import com.example.javarelay.Dto.PassengerDto;
import com.example.javarelay.Dto.Result;

public interface PassengerOrderService {

    public Result PutOrder(PassengerDto passengerDto);

    public Result CancelOrder(PassengerDto passengerDto);
    public Result FinishOrder(PassengerDto passengerDto);

}
