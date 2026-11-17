package com.example.javarelay.statustransition.strategys;

import com.example.javarelay.dto.Result;

public interface ComputeMoneyStrategy {

    public String getType();

    public Result ComputeStrategy(Long orderId);

}
