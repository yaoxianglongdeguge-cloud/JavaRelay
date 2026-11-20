package com.example.javarelay.statustransition.controller;

import com.example.javarelay.dto.Result;
import com.example.javarelay.statustransition.dto.DriverDto;
import com.example.javarelay.statustransition.service.DriverOperateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DriverOperate {

    @Autowired
    DriverOperateService driverOperateService;

    @PostMapping("/driver/order/receive")
    public Result receiveOrder(@RequestParam("driverId") Long driverId,
                           @RequestBody DriverDto driverDto){

        driverDto.setDriverId(driverId);
        return  driverOperateService.ReceiveOrder(driverDto);
    }

    @PostMapping("/driver/order/cancel")
    public Result cancelOrder(@RequestParam("driverId") Long driverId){

        return  driverOperateService.CancelOrder(driverId);
    }

    @PostMapping("/driver/order/finish")
    public Result finishOrder(@RequestParam("driverId") Long driverId){

        return  driverOperateService.FinishOrder(driverId);
    }

    @PostMapping("/driver/order/arrive")
    public Result arriveOrder(@RequestParam("driverId") Long driverId) {
        return driverOperateService.ArriveOrder(driverId);
    }

}
