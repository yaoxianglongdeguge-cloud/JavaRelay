package com.example.javarelay.pojo;

import lombok.Data;


 // 司机的"慢变属性"，与状态分开缓存。

//score 为 null 表示该司机没有 driver_detail 记录（数据异常，应告警）。

@Data
public class DriverAttr {

    private Long id;

    private Double score;
}