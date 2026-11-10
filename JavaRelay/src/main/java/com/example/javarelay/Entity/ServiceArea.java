package com.example.javarelay.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("service_area")
public class ServiceArea {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String cityName;
    private BigDecimal centerLng;
    private BigDecimal centerLat;
    private Integer radius;
    private Integer status;
}
