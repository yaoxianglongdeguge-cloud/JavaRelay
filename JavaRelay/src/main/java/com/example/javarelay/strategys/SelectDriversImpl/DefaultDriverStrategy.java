package com.example.javarelay.strategys.SelectDriversImpl;

import com.example.javarelay.dto.DriverDto;
import com.example.javarelay.dto.Result;
import com.example.javarelay.infocollect.InfoCollector;
import com.example.javarelay.strategys.SelectDriverStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DefaultDriverStrategy implements SelectDriverStrategy {

    String type = "default";

    public String getType(){
        return this.type;
    }

    @Override
    public Result findDriverStrategy(InfoCollector infoCollector) {
        return null;
    }
}
