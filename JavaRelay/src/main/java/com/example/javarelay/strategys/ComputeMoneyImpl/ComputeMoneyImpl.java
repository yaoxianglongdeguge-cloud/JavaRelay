package com.example.javarelay.strategys.ComputeMoneyImpl;

import com.example.javarelay.dto.Result;
import com.example.javarelay.infocollect.InfoCollector;
import com.example.javarelay.messagetask.MessageCells.ComputeMoneyMessage;
import com.example.javarelay.messagetask.MessageDispatcher;
import com.example.javarelay.strategys.ComputeMoneyStrategy;
import org.springframework.beans.factory.annotation.Autowired;

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
