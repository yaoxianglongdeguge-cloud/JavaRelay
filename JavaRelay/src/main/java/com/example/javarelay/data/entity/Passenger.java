package com.example.javarelay.data.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@TableName("passengers")
public class Passenger {
    @TableId(type = IdType.AUTO)
    private Long passengerId;
    @ToString.Exclude
    private String password;
    @ToString.Exclude
    private String phone;
    private String nickname;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
