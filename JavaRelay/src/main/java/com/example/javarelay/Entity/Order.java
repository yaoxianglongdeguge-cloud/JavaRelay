package com.example.javarelay.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.example.javarelay.constant.*;

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

    private Integer activeFlag;

    public Order(Long passengerId,
    Integer status){
        this.setPassengerId(passengerId);
        this.setStartLat(startLat);
        this.setStartLng(startLng);
        this.setEndLat(endLat);
        this.setEndLng(endLng);
        this.setStatus(status);
        this.setActiveFlag(Constants.ACTIVE_ORDER);
    }
}