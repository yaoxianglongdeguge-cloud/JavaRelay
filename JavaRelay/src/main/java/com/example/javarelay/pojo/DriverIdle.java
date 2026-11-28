package com.example.javarelay.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DriverIdle {

    private Long driverId;

    private LocalDateTime updateTime;

    private Double score;
}