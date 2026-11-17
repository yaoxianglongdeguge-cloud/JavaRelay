package com.example.javarelay.infra.messagetask;

import lombok.Data;

@Data
public class MessageCell {

    private String topic;
    private Object msg;

}
