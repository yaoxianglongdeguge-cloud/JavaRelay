package com.example.javarelay.statustransition.service.Impl;

import com.example.javarelay.statustransition.config.MessageTaskConfig;
import com.example.javarelay.statustransition.constants.Constants;
import com.example.javarelay.statustransition.messagetask.messagecells.DriverPatrolMessage;
import com.example.javarelay.statustransition.patroltask.DriverPatrolTask;
import com.example.javarelay.statustransition.service.DriverPatrolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DriverPatrolServiceImpl implements DriverPatrolService {

    @Autowired
    DriverPatrolTask driverPatrolTask;
    @Autowired

    @Override
    public void CheckDriverPatrolHeart() {

        long now = System.currentTimeMillis();
        long lastTs = driverPatrolTask.lastHeartbeat.get();
        long diffMs = now - lastTs;

        if(diffMs <= Constants.PATROL_HEART){
            return;
        }

        DriverPatrolMessage driverPatrolMessage = new DriverPatrolMessage();
        driverPatrolMessage.setTopic(MessageTaskConfig.DRIVER_PATROL_TOPIC);
        driverPatrolMessage.setExecuteTime(Constants.PATROL_END_TIME);


    }
}
