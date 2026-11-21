package com.example.javarelay.data.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@TableName("driver_detail")
public class DriverDetail {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long driverId;


    private String nickname;

    @ToString.Exclude
    private String password;
    @ToString.Exclude
    private String phone;

    private Double score;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
