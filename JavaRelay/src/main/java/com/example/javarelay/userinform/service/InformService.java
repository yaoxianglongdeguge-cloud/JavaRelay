package com.example.javarelay.userinform.service;

import com.example.javarelay.dto.Result;

public interface InformService {

    public Result informPassenger(Long orderId,Long meId,Long toId,Integer msgType,String message);

    public Result informDriver(Long orderId,Long meId,Long toId,Integer msgType,String message);

}
