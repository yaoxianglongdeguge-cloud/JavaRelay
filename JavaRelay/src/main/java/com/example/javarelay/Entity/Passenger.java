package com.example.javarelay.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("passenger")
public class Passenger {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String phone;
    private String password;
    private String nickname;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
