package com.example.javarelay.service.Impl;

import com.example.javarelay.constant.Constants;
import com.example.javarelay.dto.Result;
import com.example.javarelay.messagetask.MessageCells.InformMessage;
import com.example.javarelay.messagetask.MessageDispatcher;
import com.example.javarelay.service.InformService;
import org.springframework.beans.factory.annotation.Autowired;

public class InformServiceImpl implements InformService {

    @Autowired
    MessageDispatcher messageDispatcher;

    @Override
    public Result informPassenger(String message) {

        InformMessage informMessage = new InformMessage(Constants.TO_PASSENGER,message);
        informMessage.setTopic(Constants.MESSAGE_INFORM_TOPIC);



        return null;
    }

    @Override
    public Result informDriver(String message) {
        return null;
    }
}
