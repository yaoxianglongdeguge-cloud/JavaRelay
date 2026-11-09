package com.example.javarelay.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("order_status_log")
public class OrderStatusLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Integer fromStatus;
    private Integer toStatus;
    private Integer operatorType; // 1乘客, 2司机, 3系统
    private Long operatorId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public OrderStatusLog(Long orderId,Integer fromStatus,Integer toStatus,Integer operatorType,Long operatorId){
        this.orderId = orderId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.operatorType = operatorType;
        this.operatorId = operatorId;
    }
}