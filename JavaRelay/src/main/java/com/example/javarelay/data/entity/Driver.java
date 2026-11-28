package com.example.javarelay.data.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("drivers")
public class Driver {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 0空闲, 1接单中
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // 每次更新自动刷新，空闲司机用它算空闲时长
    private LocalDateTime updateTime;
}