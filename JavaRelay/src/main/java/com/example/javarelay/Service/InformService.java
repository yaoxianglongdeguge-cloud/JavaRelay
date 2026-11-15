package com.example.javarelay.service;

import com.example.javarelay.dto.Result;

public interface InformService {

    public Result informPassenger(String message);

    public Result informDriver(String message);

}
