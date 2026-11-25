package com.example.javarelay.data.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.example.javarelay.orderstatus.pojo.RouteInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import com.alibaba.fastjson2.JSON;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_detail")
public class OrderDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;
    private Long passengerId;

    @JsonIgnore // 防止 Controller 返回数据时把这个大字符串暴露给前端
    private String routeSnapshot;


    @TableField(exist = false)
    private RouteInfo routeInfo;


    private BigDecimal estimatedPrice;
    private BigDecimal actualPrice;

    /**
     * 数据阶段：0=下单只有起终点，1=已派单，2=路线与预估价就绪，null=已冻结（取消或结算完成）。
     * 所有写入都必须带 {@code WHERE data_stage = 前值}，null 天然挡住一切写入。
     */
    private Integer dataStage;

    private String acpriceType;
    private String espriceType;
    private String driverType;
    private String passengerType;
    private String pathType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public OrderDetail(){
        this.routeInfo = null;
    }


    public void serializeRouteInfo(RouteInfo info) {
        if (info != null) {
            this.routeInfo = info;
            this.routeSnapshot = JSON.toJSONString(info);
        }
    }

    public RouteInfo deserializeRouteInfo() {
        if (this.routeSnapshot != null && !this.routeSnapshot.isEmpty()) {
            this.routeInfo = JSON.parseObject(this.routeSnapshot, RouteInfo.class);
        }
        return this.routeInfo;
    }
}
