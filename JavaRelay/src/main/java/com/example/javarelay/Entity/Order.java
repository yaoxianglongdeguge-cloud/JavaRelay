package com.example.javarelay.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ride_order")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long passengerId;

    private BigDecimal startLng;
    private BigDecimal startLat;
    private BigDecimal endLng;
    private BigDecimal endLat;

    private Integer status;

    private Long driverId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public Order(Long passengerId,BigDecimal startLng,BigDecimal startLat,BigDecimal endLat,BigDecimal endLng,
    Integer status){
        this.setPassengerId(passengerId);
        this.setStartLat(startLat);
        this.setStartLng(startLng);
        this.setEndLat(endLat);
        this.setEndLng(endLng);
        this.setStatus(status);
    }
}