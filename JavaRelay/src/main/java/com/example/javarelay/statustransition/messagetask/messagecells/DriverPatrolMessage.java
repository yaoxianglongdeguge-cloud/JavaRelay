package com.example.javarelay.statustransition.messagetask.messagecells;

import com.example.javarelay.infra.messagetask.MessageCell;
import com.example.javarelay.statustransition.config.MessageTaskConfig;
import lombok.Data;

public class DriverPatrolMessage extends MessageCell {

    public DriverPatrolMessage(){
        this.setTopic(MessageTaskConfig.DRIVER_PATROL_TOPIC);
    }
}
