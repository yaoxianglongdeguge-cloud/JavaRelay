package com.example.javarelay.infra.messagetask;


public interface MessageHandler {

    public String getTopic();

    public Boolean handler(MessageCell delayMessageCell);

}
