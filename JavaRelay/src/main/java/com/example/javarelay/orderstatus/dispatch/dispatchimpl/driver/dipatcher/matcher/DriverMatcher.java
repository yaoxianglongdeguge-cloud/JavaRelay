package com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.dipatcher.matcher;

import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.pojo.DriverCandidate;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.pojo.DriverMatchJob;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.pojo.DriverMatchResult;

import java.util.List;

public interface DriverMatcher {

    List<DriverMatchResult> match(List<DriverMatchJob> orders, List<DriverCandidate> drivers,
                                  double[][] weights);
}