package com.example.javarelay.data.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("driver_detail")
public class DriverDetail {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long driverId;

    private String phone;

    private String nickname;

    private String password;

    private Double score;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
