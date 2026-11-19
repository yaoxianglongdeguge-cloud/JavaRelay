package com.example.javarelay.statustransition.controller;

import com.example.javarelay.data.entity.Passenger;
import com.example.javarelay.dto.Result;
import com.example.javarelay.statustransition.dto.PassengerDto;
import com.example.javarelay.statustransition.service.PassengerOperateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PassengerOperate {
    @Autowired
    PassengerOperateService passengerOperateService;

    @PostMapping("/passenger/order")
    public Result putOrder(@RequestParam("passengerId") Long passengerId,
                           @RequestBody PassengerDto passengerDto){

        passengerDto.setPassengerId(passengerId);
        return  passengerOperateService.PutOrder(passengerDto);
    }

    @PostMapping("/passenger/order/cancel")
    public Result cancelOrder(@RequestParam("passengerId") Long passengerId){

        return passengerOperateService.CancelOrder(passengerId);

    }
    @PostMapping("/passenger/order/finish")
    public Result finishOrder(@RequestParam("passengerId") Long passengerId){

        return passengerOperateService.FinishOrder(passengerId);

    }

    @PostMapping("/passenger/order/recompute")
    public Result reComputeMoney(@RequestParam("orderId") Long orderId){

        return  passengerOperateService.ReComputeMoney(orderId);
    }
}
