package com.example.javarelay.service;

import com.example.javarelay.dto.PassengerDto;

public interface OrderService {

    public Boolean createAndInsertOrder(PassengerDto passengerDto);

}
