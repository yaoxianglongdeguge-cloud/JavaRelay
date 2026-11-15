package com.example.javarelay.messagetask;

public interface MessageHandler {

    public String getTopic();

    public Boolean handler(MessageCell messageCell);

}
