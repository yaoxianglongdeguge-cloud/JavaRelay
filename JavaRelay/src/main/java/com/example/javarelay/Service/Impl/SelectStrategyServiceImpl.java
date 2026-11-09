package com.example.javarelay.service.Impl;

import com.example.javarelay.dto.DriverDto;
import com.example.javarelay.selectstrategy.SelectStrategy;
import com.example.javarelay.service.SelectStrategyService;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
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
