package com.example.javarelay.infra.mq;


public interface MessageHandler {

    public String getTopic();

    public Boolean handler(MessageCell delayMessageCell);

}
