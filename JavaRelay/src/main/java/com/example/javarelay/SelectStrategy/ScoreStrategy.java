package com.example.javarelay.selectstrategy;

import com.example.javarelay.dto.DriverDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScoreStrategy implements SelectStrategy {
    @Override
    public List<DriverDto> findByStrategy() {
        return List.of();
    }
}
