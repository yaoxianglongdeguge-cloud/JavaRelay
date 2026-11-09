package com.example.javarelay.Service;

import com.example.javarelay.Dto.PassengerDto;
import com.example.javarelay.Entity.Passenger;

import java.math.BigDecimal;

public interface OrderService {

    public Boolean createAndInsertOrder(PassengerDto passengerDto);

}
