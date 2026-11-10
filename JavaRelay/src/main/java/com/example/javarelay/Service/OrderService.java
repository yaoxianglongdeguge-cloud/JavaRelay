package com.example.javarelay.service;

import com.example.javarelay.dto.PassengerDto;
import com.example.javarelay.dto.Result;

public interface OrderService {

    public Result createAndInsertOrder(PassengerDto passengerDto);

}
