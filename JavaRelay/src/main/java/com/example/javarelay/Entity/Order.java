package com.example.javarelay.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.example.javarelay.constant.*;

@Data
@TableName("orders")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long passengerId;

    private Integer status;

    private Long driverId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    //唯一索引，用来插入订单时防止重复
    private Integer activeFlag;

    public Order(Long passengerId,
    Integer status){
        this.setPassengerId(passengerId);
        this.setStatus(status);
        this.setActiveFlag(Constants.ACTIVE_ORDER);
    }
}