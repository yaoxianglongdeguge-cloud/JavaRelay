package com.example.javarelay.inform.service.impl;

import com.example.javarelay.config.RootInformConfig;
import com.example.javarelay.inform.service.InformService;
import com.example.javarelay.statustransition.config.MessageTaskConfig;
import com.example.javarelay.statustransition.constants.Constants;
import com.example.javarelay.dto.Result;
import com.example.javarelay.inform.massagetask.messagecells.InformMessage;
import com.example.javarelay.infra.messagetask.MessageDispatcher;
import org.springframework.beans.factory.annotation.Autowired;

public class InformServiceImpl implements InformService {

    @Autowired
    MessageDispatcher messageDispatcher;

    @Override
    public Result informPassenger(Long orderId,Long toId,Integer msgType,String message) {

        InformMessage informMessage = new InformMessage(RootInformConfig.TO_PASSENGER,orderId,toId,msgType,message);
        informMessage.setTopic(MessageTaskConfig.MESSAGE_INFORM_TOPIC);
        messageDispatcher.putMessage(informMessage);

        return Result.success("发送成功");
    }

    @Override
    public Result informDriver(Long orderId,Long toId,Integer msgType,String message) {

        InformMessage informMessage = new InformMessage(RootInformConfig.TO_DRIVER,orderId,toId,msgType,message);
        informMessage.setTopic(MessageTaskConfig.MESSAGE_INFORM_TOPIC);
        messageDispatcher.putMessage(informMessage);

        return Result.success("发送成功");
    }
}
