package com.example.javarelay.infocollect;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Data
public class InfoCollector {

    Long orderId;

    private String selectPassengerStrategy;
    private Integer passengerStrategyMark;

    private String selectDriverStrategy;
    private Integer driverStrategyMark;

    private String precomputeMoneyType;
    private Integer precomMark;

    private String computeMoneyType;
    private Integer computeMark;

    private PassengerInfo passengerInfo;

    @Data
    public class PassengerInfo{

        Long passengerId;

        private BigDecimal startLng;
        private BigDecimal startLat;
        private BigDecimal endLng;
        private BigDecimal endLat;
        private String city;
    }
}
