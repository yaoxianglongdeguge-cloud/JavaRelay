package com.example.javarelay.infocollect;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Data
public class InfoCollector {

    private Long orderId;

    private String selectPassengerStrategy;
    private Integer passengerStrategyMark = null;

    private String selectDriverStrategy;
    private Integer driverStrategyMark = null;

    private String selectPathStrategyType;
    private Integer pathStrategyMark = null;

    private String precomputeMoneyType;
    private Integer precomMark = null;

    private String computeMoneyType;
    private Integer computeMark = null;


    private Long passengerId;

    private BigDecimal startLng;
    private BigDecimal startLat;
    private String startAddress;


    private BigDecimal endLng;
    private BigDecimal endLat;
    private String endAddress;


}
