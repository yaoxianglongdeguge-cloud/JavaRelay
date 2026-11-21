package com.example.javarelay.strategysimpl.infocollect;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Data
public class InfoCollector {

    private Long orderId;

    private String selectPassengerStrategy;

    private String selectDriverStrategy;

    private String selectPathStrategyType ;

    private String precomputeMoneyType ;

    private String computeMoneyType;

    private Long passengerId;

    private BigDecimal startLng;
    private BigDecimal startLat;
    private String startAddress;


    private BigDecimal endLng;
    private BigDecimal endLat;
    private String endAddress;


}
