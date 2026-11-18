package com.example.javarelay.statustransition.service;

import com.example.javarelay.data.entity.ServiceArea;
import com.example.javarelay.dto.Result;
import com.example.javarelay.statustransition.dto.PassengerDto;

public interface ServeAreaService {

    public Result changeServeArea(ServiceArea serviceArea);

    public Result getServeArea(String city);
}
