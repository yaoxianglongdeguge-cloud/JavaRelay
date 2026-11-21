package com.example.javarelay.orderstatus.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.example.javarelay.dto.Result;
import com.example.javarelay.orderstatus.dto.PassengerDto;
import com.example.javarelay.orderstatus.service.PassengerOperateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class PassengerOperate {
    @Autowired
    PassengerOperateService passengerOperateService;


    @SentinelResource(value = "order:put", blockHandler = "putBlocked")
    @PostMapping("/passenger/order")
    public Result putOrder(@RequestAttribute("userId") Long passengerId,
                           @RequestBody PassengerDto passengerDto) {
        passengerDto.setPassengerId(passengerId);
        return passengerOperateService.PutOrder(passengerDto);
    }

    public Result putBlocked(Long passengerId, PassengerDto passengerDto, BlockException e) {
        return Result.tooManyRequests("下单太频繁，请稍后再试");
    }

    @SentinelResource(value = "order:cancel", blockHandler = "cancelBlocked")
    @PostMapping("/passenger/order/cancel")
    public Result cancelOrder(@RequestAttribute("userId") Long passengerId) {
        return passengerOperateService.CancelOrder(passengerId);
    }

    public Result cancelBlocked(Long passengerId, BlockException e) {
        return Result.tooManyRequests("操作太频繁，请稍后再试");
    }

    @SentinelResource(value = "order:finish", blockHandler = "finishBlocked")
    @PostMapping("/passenger/order/finish")
    public Result finishOrder(@RequestAttribute("userId") Long passengerId) {
        return passengerOperateService.FinishOrder(passengerId);
    }

    public Result finishBlocked(Long passengerId, BlockException e) {
        return Result.tooManyRequests("操作太频繁，请稍后再试");
    }
}
