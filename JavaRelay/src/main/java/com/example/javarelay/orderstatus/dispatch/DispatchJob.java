package com.example.javarelay.orderstatus.dispatch;

import com.example.javarelay.pojo.DispatchMetric;
import com.example.javarelay.strategysimpl.infocollect.InfoCollector;
import lombok.Data;

/**
 * 队列里的一单：订单 + 它自己的策略指标。
 *
 * <p>替代了 {@code BatchDispatchTask.PendingDispatch}，让任务类不再持有内部数据结构。
 */
@Data
public class DispatchJob {

    private Long orderId;

    private Long passengerId;

    /** 订单上下文（坐标、策略类型等） */
    private InfoCollector ctx;

    /** 进队时由策略注入的指标（权重） */
    private DispatchMetric metric;

    /** 入队时间戳，用于超时判断 */
    private long enqueueAt;
}