package com.example.javarelay.datamanage.service;

import com.example.javarelay.data.entity.ServiceArea;
import com.example.javarelay.dto.Result;

public interface ServeAreaService {

    public Result changeServeArea(ServiceArea serviceArea);

    public Result getServeArea(String city);

    public boolean setServiceAreaRedis(String city,Object data,long seconds);

    public ServiceArea getServiceAreaRedis(String city);
}
