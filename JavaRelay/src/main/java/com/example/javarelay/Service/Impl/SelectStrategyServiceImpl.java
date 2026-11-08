package com.example.javarelay.Service.Impl;

import com.example.javarelay.Dto.DriverDto;
import com.example.javarelay.SelectStrategy.SelectStrategy;
import com.example.javarelay.Service.SelectStrategyService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SelectStrategyServiceImpl implements SelectStrategyService {

    Map<String,SelectStrategy> map;

    public SelectStrategyServiceImpl(Map<String,SelectStrategy> map){
        this.map = map;
    }

    @Override
    public List<DriverDto> findByStrategy(String type) {
        return map.get(type).findByStrategy();
    }
}
