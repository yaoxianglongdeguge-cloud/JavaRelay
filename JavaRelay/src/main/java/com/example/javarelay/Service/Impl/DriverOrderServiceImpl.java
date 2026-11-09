package com.example.javarelay.service.Impl;

import com.example.javarelay.dto.DriverDto;
import com.example.javarelay.dto.Result;
import com.example.javarelay.service.DriverOrderService;
import org.springframework.stereotype.Service;

@Service
public class DriverOrderServiceImpl implements DriverOrderService {

    @Override
    public Result CancelOrder(DriverDto driverDto) {
        return null;
    }

    @Override
    public Result FinishOrder(DriverDto driverDto) {
        return null;
    }

    @Override
    public Result ReceiverOrder(DriverDto driverDto) {
        return null;
    }
}
