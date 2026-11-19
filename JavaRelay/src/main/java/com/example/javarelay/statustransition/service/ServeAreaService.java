package com.example.javarelay.statustransition.service;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.data.entity.ServiceArea;
import com.example.javarelay.dto.Result;
import com.example.javarelay.rediscommon.RedisLogicTimeout;
import com.example.javarelay.rediscommon.RedisPefix;
import com.example.javarelay.statustransition.dto.PassengerDto;

import java.time.LocalDateTime;

public interface ServeAreaService {

    public Result changeServeArea(ServiceArea serviceArea);

    public Result getServeArea(String city);

    public boolean setServiceAreaRedis(String city,Object data,long seconds);

    public ServiceArea getServiceAreaRedis(String city);
}
