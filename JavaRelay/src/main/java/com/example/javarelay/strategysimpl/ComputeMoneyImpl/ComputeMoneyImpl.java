package com.example.javarelay.strategysimpl.ComputeMoneyImpl;

import com.example.javarelay.dto.Result;
import com.example.javarelay.statustransition.strategys.ComputeMoneyStrategy;

public class ComputeMoneyImpl implements ComputeMoneyStrategy {

    String type = "default";


    public String getType(){
        return this.type;
    }

    @Override
    public Result ComputeStrategy(Long orderId) {



        return Result.success();
    }

}
