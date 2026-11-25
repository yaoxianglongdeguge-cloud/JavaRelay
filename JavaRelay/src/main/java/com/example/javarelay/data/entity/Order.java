package com.example.javarelay.data.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.javarelay.orderstatus.constants.Constants;
import lombok.Data;

import java.time.LocalDateTime;

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

    public Order(Long passengerId){
        this.setPassengerId(passengerId);
    }
}