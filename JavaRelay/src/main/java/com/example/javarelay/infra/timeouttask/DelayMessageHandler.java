package com.example.javarelay.infra.timeouttask;

import com.example.javarelay.infra.messagetask.MessageCell;

public interface DelayMessageHandler {

    public String getTopic();

    public Boolean handler(DelayMessageCell delayMessageCell);

}
