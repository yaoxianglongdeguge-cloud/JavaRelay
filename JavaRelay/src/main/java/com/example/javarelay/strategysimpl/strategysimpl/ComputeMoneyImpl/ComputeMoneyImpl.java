package com.example.javarelay.strategysimpl.strategysimpl.ComputeMoneyImpl;

import com.example.javarelay.dto.Result;
import com.example.javarelay.strategysapi.ComputeMoneyStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ComputeMoneyImpl implements ComputeMoneyStrategy {

    String type = "default";


    public String getType(){
        return this.type;
    }

    @Override
    public Result ComputeStrategy(Long orderId) {

        BigDecimal a = new BigDecimal(100);

        return Result.success(a);
    }

}
