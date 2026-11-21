package com.example.javarelay.orderstatus.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.example.javarelay.dto.Result;
import com.example.javarelay.orderstatus.dto.DriverDto;
import com.example.javarelay.orderstatus.service.DriverOperateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class DriverOperate {

    @Autowired
    DriverOperateService driverOperateService;

    @SentinelResource(value = "order:receive", blockHandler = "receiveBlocked")
    @PostMapping("/driver/order/receive")
    public Result receiveOrder(@RequestAttribute("userId") Long driverId,
                               @RequestBody DriverDto driverDto) {
        driverDto.setDriverId(driverId);
        return driverOperateService.ReceiveOrder(driverDto);
    }

    public Result receiveBlocked(Long driverId, DriverDto driverDto, BlockException e) {
        return Result.tooManyRequests("操作太频繁，请稍后再试");
    }

    @SentinelResource(value = "order:arrive", blockHandler = "arriveBlocked")
    @PostMapping("/driver/order/arrive")
    public Result arriveOrder(@RequestAttribute("userId") Long driverId) {
        return driverOperateService.ArriveOrder(driverId);
    }

    public Result arriveBlocked(Long driverId, BlockException e) {
        return Result.tooManyRequests("操作太频繁，请稍后再试");
    }

    @SentinelResource(value = "order:driverFinish", blockHandler = "driverFinishBlocked")
    @PostMapping("/driver/order/finish")
    public Result finishOrder(@RequestAttribute("userId") Long driverId) {
        return driverOperateService.FinishOrder(driverId);
    }

    public Result driverFinishBlocked(Long driverId, BlockException e) {
        return Result.tooManyRequests("操作太频繁，请稍后再试");
    }

}
