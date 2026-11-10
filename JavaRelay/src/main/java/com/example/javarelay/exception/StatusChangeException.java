package com.example.javarelay.exception;

public class StatusChangeException extends RuntimeException {
    private final Integer code;

    public StatusChangeException(String message) {
        super(message);
        this.code = 478;
    }

    public StatusChangeException(Integer code, String message) {
        super(message);
        this.code = code;
    }

}
