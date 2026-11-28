package com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.metric;

import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.pojo.DriverCandidate;

public interface DriverMetric {

    String type();

    // 越高越优先；<= 0 表示不可用
    double score(DriverCandidate candidate);
}