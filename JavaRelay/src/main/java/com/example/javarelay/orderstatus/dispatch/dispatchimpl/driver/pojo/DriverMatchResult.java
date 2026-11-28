package com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.pojo;

import lombok.Data;

@Data
public class DriverMatchResult {

    private DriverMatchJob job;

    private DriverCandidate driver;

    private double weight;
}