package com.example.javarelay.data.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@TableName("service_area")
@EqualsAndHashCode(exclude = {"id"})
public class ServiceArea {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private String cityName;

    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private BigDecimal centerLng;

    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private BigDecimal centerLat;

    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private Integer radius;

    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private Integer priceLevel;


}
