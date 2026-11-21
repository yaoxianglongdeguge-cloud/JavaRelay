package com.example.javarelay.strategysimpl.strategysimpl.PreComputeMoneyImpl;

import com.example.javarelay.dto.Result;
import com.example.javarelay.strategysimpl.infocollect.InfoCollector;
import com.example.javarelay.strategysapi.PreComputeStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PreComputeMoneyImpl implements PreComputeStrategy {

    String type = "default";


    public String getType(){
        return this.type;
    }

    @Override
    public Result PreComputeStrategy(InfoCollector infoCollector) {
        BigDecimal a = new BigDecimal(100);

        return Result.success(a);
    }

}
