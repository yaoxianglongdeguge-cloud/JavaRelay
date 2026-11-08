package com.example.javarelay.Service.Impl;

import com.example.javarelay.Dto.PassengerDto;
import com.example.javarelay.Dto.Result;
import com.example.javarelay.SelectStrategy.SelectStrategy;
import com.example.javarelay.Service.PassengerOrderService;
import com.example.javarelay.Service.SelectStrategyService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

public class PassengerOrderServiceImpl implements PassengerOrderService {

    @Autowired
    SelectStrategyService selectStrategyService;

    @Override
    public Result PutOrder(PassengerDto passengerDto) {
        return null;
    }

    @Override
    public Result CancelOrder(PassengerDto passengerDto) {
        return null;
    }

    @Override
    public Result FinishOrder(PassengerDto passengerDto) {
        return null;
    }

}
