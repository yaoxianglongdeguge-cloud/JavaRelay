package com.example.javarelay.orderstatus.dispatch;

import com.example.javarelay.strategysimpl.infocollect.InfoCollector;
import lombok.Data;

/**
 * 队列里的一单：订单 + 上下文 + 入队时间。
 */
@Data
public class DispatchJob {

    private Long orderId;

    private Long passengerId;

    /** 订单上下文（坐标、策略类型等） */
    private InfoCollector ctx;

    /** 入队时间戳，用于超时判断 */
    private long enqueueAt;
}