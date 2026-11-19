package com.example.javarelay.inform.service;

import com.example.javarelay.dto.Result;
import org.springframework.stereotype.Service;

public interface InformService {

    public Result informPassenger(Long orderId,Long meId,Long toId,Integer msgType,String message);

    public Result informDriver(Long orderId,Long meId,Long toId,Integer msgType,String message);

}
