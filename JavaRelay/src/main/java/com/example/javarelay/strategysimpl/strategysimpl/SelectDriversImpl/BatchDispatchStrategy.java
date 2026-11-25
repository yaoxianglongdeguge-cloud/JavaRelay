package com.example.javarelay.strategysimpl.strategysimpl.SelectDriversImpl;

import com.example.javarelay.dto.Result;
import com.example.javarelay.orderstatus.dispatch.DispatchJob;
import com.example.javarelay.orderstatus.dispatch.DispatchQueue;
import com.example.javarelay.pojo.DispatchMetric;
import com.example.javarelay.pojo.DispatchMetricPreset;
import com.example.javarelay.strategysapi.SelectDriverStrategy;
import com.example.javarelay.strategysimpl.infocollect.InfoCollector;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 统一分配策略：只做一件事——把订单连同指标塞进队列。
 *
 * <p>返回值语义唯一：<b>有没有入队</b>。
 * 入队之后怎么匹配、匹配不到怎么办，都不属于策略的职责。
 *
 * <p>三个策略（default:1/2/3）共用本类，差别只在 {@link DispatchMetricPreset} 的权重。
 */
public class BatchDispatchStrategy implements SelectDriverStrategy {

    private final String type;

    private final DispatchMetric metric;

    @Autowired
    DispatchQueue dispatchQueue;

    public BatchDispatchStrategy(String type) {
        this.type = type;
        this.metric = DispatchMetricPreset.of(type).metric();
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Result findDriverStrategy(InfoCollector infoCollector) {

        if (infoCollector == null || infoCollector.getOrderId() == null) {
            return Result.fail("订单上下文缺失");
        }

        DispatchJob job = new DispatchJob();
        job.setOrderId(infoCollector.getOrderId());
        job.setPassengerId(infoCollector.getPassengerId());
        job.setCtx(infoCollector);
        job.setMetric(metric);
        job.setEnqueueAt(System.currentTimeMillis());

        if (!dispatchQueue.offer(job)) {
            return Result.fail("分配队列已满");
        }

        return Result.success("已加入分配队列：" + metric.getName());
    }
}