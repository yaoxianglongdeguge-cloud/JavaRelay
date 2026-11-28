package com.example.javarelay.orderstatus.dispatch.dto;

import com.example.javarelay.orderstatus.pojo.RouteInfo;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DispatchData {

    private Long driverId;

    private RouteInfo route;

    private BigDecimal estimatedPrice;

    // 非空表示这个环节失败
    private String failReason;
}