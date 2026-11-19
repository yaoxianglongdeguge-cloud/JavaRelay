package com.example.javarelay.inform.service.impl;

import com.example.javarelay.config.RootInformConfig;
import com.example.javarelay.inform.config.MessageTaskConfig;
import com.example.javarelay.inform.service.InformService;
import com.example.javarelay.statustransition.constants.Constants;
import com.example.javarelay.dto.Result;
import com.example.javarelay.inform.massagetask.messagecells.InformMessage;
import com.example.javarelay.infra.messagetask.MessageDispatcher;
import org.springframework.beans.factory.annotation.Autowired;

public class InformServiceImpl implements InformService {

    @Autowired
    MessageDispatcher messageDispatcher;

    @Override
    public Result informPassenger(Long orderId,Long meId,Long toId,Integer msgType,String message) {

        InformMessage informMessage = new InformMessage(RootInformConfig.TO_PASSENGER,orderId,meId,toId,msgType,message);
        messageDispatcher.addTask(informMessage);

        return Result.success("发送成功");
    }

    @Override
    public Result informDriver(Long orderId,Long meId,Long toId,Integer msgType,String message) {

        InformMessage informMessage = new InformMessage(RootInformConfig.TO_DRIVER,orderId,meId,toId,msgType,message);
        messageDispatcher.addTask(informMessage);

        return Result.success("发送成功");
    }
}
