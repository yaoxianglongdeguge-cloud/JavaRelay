package com.example.javarelay.statustransition.controller;

import com.example.javarelay.data.entity.Passenger;
import com.example.javarelay.dto.Result;
import com.example.javarelay.statustransition.dto.PassengerDto;
import com.example.javarelay.statustransition.service.PassengerOperateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class PassengerOperate {
    @Autowired
    PassengerOperateService passengerOperateService;

    @PostMapping()
    public Result putOrder(@RequestAttribute("passengerId") Long passengerId,
                           @RequestBody PassengerDto passengerDto){

        passengerDto.setPassengerId(passengerId);
        return  passengerOperateService.PutOrder(passengerDto);
    }

    public Result cancelOrder(@RequestAttribute("passengerId") Long passengerId){

        return passengerOperateService.CancelOrder(passengerId);

    }
    public Result finishOrder(@RequestAttribute("passengerId") Long passengerId){

        return passengerOperateService.FinishOrder(passengerId);

    }

    public Result reComputeMoney(@RequestAttribute("passengerId") Long passengerId){

        return  passengerOperateService.ReComputeMoney(passengerId);
    }
}
