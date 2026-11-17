package com.example.javarelay.statustransition.controller;

import com.example.javarelay.dto.Result;
import com.example.javarelay.statustransition.dto.DriverDto;
import com.example.javarelay.statustransition.dto.PassengerDto;
import com.example.javarelay.statustransition.service.DriverOperateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;

public class DriverOperate {

    @Autowired
    DriverOperateService driverOperateService;

    @PostMapping()
    public Result receiveOrder(@RequestAttribute("driverId") Long driverId,
                           @RequestBody DriverDto driverDto){

        driverDto.setDriverId(driverId);
        return  driverOperateService.ReceiveOrder(driverDto);
    }

    @PostMapping()
    public Result cancelOrder(@RequestAttribute("driverId") Long driverId){

        return  driverOperateService.CancelOrder(driverId);
    }

    @PostMapping()
    public Result finishOrder(@RequestAttribute("driverId") Long driverId){

        return  driverOperateService.FinishOrder(driverId);
    }

}
