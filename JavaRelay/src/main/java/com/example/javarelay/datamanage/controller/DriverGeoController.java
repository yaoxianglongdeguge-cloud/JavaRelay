package com.example.javarelay.datamanage.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.example.javarelay.datamanage.dto.DriverGeoDto;
import com.example.javarelay.datamanage.service.DriverGeoService;
import com.example.javarelay.dto.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 司机位置上报入口。司机端建议 3 秒一次。
 */
@RestController
public class DriverGeoController {

    @Autowired
    DriverGeoService driverGeoService;

    @SentinelResource(value = "driver:location", blockHandler = "locationBlocked")
    @PostMapping("/driver/location")
    public Result report(@RequestAttribute("userId") Long driverId,
                         @RequestBody DriverGeoDto dto) {

        if (dto == null) {
            return Result.fail("位置信息不全");
        }

        return driverGeoService.report(driverId, dto.getLng(), dto.getLat());
    }

    public Result locationBlocked(Long driverId, DriverGeoDto dto, BlockException e) {
        return Result.tooManyRequests("上报太频繁，请稍后再试");
    }
}