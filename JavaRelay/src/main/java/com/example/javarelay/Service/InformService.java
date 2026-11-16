package com.example.javarelay.service;

import com.example.javarelay.dto.Result;

public interface InformService {

    public Result informPassenger(Integer msgType,String message);

    public Result informDriver(Integer msgType,String message);

}
