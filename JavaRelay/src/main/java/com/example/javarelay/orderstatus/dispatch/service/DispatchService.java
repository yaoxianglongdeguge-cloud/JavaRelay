package com.example.javarelay.orderstatus.dispatch.service;

import com.example.javarelay.dto.Result;

public interface DispatchService {

    Result dispatch(Long orderId, Long passengerId);

    Result reDispatch(Long orderId, Long passengerId);

    Result assignPath(Long orderId, Long passengerId);
}