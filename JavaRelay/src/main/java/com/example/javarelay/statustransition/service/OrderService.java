package com.example.javarelay.statustransition.service;

import com.example.javarelay.statustransition.dto.PassengerDto;
import com.example.javarelay.dto.Result;

public interface OrderService {

    public Result createAndInsertOrder(PassengerDto passengerDto);

    public Result changeOrderStatus(Long orderId,Long passengerId,
                                    Integer fromStatus,Integer toStatus,Integer operator,
                                    Long driverId,Integer fromStatus2,Integer toStatus2);

}
