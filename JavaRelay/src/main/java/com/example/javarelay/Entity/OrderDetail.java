package com.example.javarelay.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.example.javarelay.dto.RouteInfo;
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
    private Long driverId;

    @JsonIgnore // 防止 Controller 返回数据时把这个大字符串暴露给前端
    private String routeSnapshot;


    @TableField(exist = false)
    private RouteInfo routeInfo;


    private BigDecimal estimatedPrice;
    private BigDecimal actualPrice;
    private String pricingRuleSnapshot;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public OrderDetail(){
        this.routeInfo = null;
    }



    /**
     * 写入前调用：将 RouteInfo 对象转成 JSON，存进 routeSnapshot
     */
    public void serializeRouteInfo(RouteInfo info) {
        if (info != null) {
            this.routeInfo = info;
            this.routeSnapshot = JSON.toJSONString(info);
        }
    }

    /**
     * 读取后调用：将 routeSnapshot 里的 JSON 还原成 RouteInfo 对象
     */
    public RouteInfo deserializeRouteInfo() {
        if (this.routeSnapshot != null && !this.routeSnapshot.isEmpty()) {
            this.routeInfo = JSON.parseObject(this.routeSnapshot, RouteInfo.class);
        }
        return this.routeInfo;
    }
}