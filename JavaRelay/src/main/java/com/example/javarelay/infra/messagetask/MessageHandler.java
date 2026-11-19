package com.example.javarelay.infra.messagetask;


import java.util.List;

public interface MessageHandler {

    public String getTopic();

    public Boolean handler(MessageCell delayMessageCell);

}
