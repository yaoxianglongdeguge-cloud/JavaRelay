package com.example.javarelay.SelectStrategy;

import com.example.javarelay.Dto.DriverDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScoreStrategy implements SelectStrategy {
    @Override
    public List<DriverDto> findByStrategy() {
        return List.of();
    }
}
