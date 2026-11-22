package com.example.javarelay.infra.mq;

import jakarta.annotation.Nonnull;
import lombok.Data;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Data
public class MessageCell<T> {

    private String topic;
    private long executeTime;
    private T msg;

}