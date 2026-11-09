package com.example.javarelay.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("driver")
public class Driver {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String phone;
    // 0空闲, 1接单中
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
