package com.example.javarelay.orderstatus.service;

import com.example.javarelay.dto.Result;
import com.example.javarelay.orderstatus.dto.PassengerDto;

import java.math.BigDecimal;

public interface OrderService {

    Result createAndInsertOrder(PassengerDto passengerDto);

    boolean dispatchOrder(Long orderId, Long passengerId, Long driverId);

    boolean saveRouteAndPrice(Long orderId, Long driverId, BigDecimal estimatedPrice, String routeSnapshot);

    boolean receiveOrder(Long orderId, Long passengerId, Long driverId);

    boolean arriveOrder(Long orderId, Long passengerId, Long driverId);

    boolean finishOrder(Long orderId, Long passengerId, Long driverId);

    boolean settleOrder(Long orderId, BigDecimal actualPrice);

    boolean cancelOrder(Long orderId, Long passengerId, Integer fromStatus, Long driverId,
                        Integer operator, String reason);

    boolean rollbackForRedispatch(Long orderId, Long passengerId, Long driverId, Integer operator);
}