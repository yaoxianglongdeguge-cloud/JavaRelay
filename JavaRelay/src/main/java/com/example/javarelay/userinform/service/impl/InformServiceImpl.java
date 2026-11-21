package com.example.javarelay.userinform.service.impl;

import com.example.javarelay.globalconfig.RootInformConfig;
import com.example.javarelay.userinform.service.InformService;
import com.example.javarelay.dto.Result;
import com.example.javarelay.userinform.mqtask.messagecells.InformMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class InformServiceImpl implements InformService {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public Result informPassenger(Long orderId,Long meId,Long toId,Integer msgType,String message) {

        InformMessage informMessage = new InformMessage(RootInformConfig.TO_PASSENGER,orderId,meId,toId,msgType,message);
        eventPublisher.publishEvent(informMessage);

        return Result.success("发送成功");
    }

    @Override
    public Result informDriver(Long orderId,Long meId,Long toId,Integer msgType,String message) {

        InformMessage informMessage = new InformMessage(RootInformConfig.TO_DRIVER,orderId,meId,toId,msgType,message);
        eventPublisher.publishEvent(informMessage);

        return Result.success("发送成功");
    }
}
