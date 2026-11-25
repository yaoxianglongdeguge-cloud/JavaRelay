package com.example.javarelay.strategysimpl.strategysimpl.SelectDriversImpl;

import com.example.javarelay.dto.Result;
import com.example.javarelay.orderstatus.dispatch.DispatchJob;
import com.example.javarelay.orderstatus.dispatch.DispatchQueue;
import com.example.javarelay.strategysapi.SelectDriverStrategy;
import com.example.javarelay.strategysimpl.infocollect.InfoCollector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class BatchDispatchStrategy implements SelectDriverStrategy {

    private final String type = "default";

    @Autowired
    DispatchQueue dispatchQueue;

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
        job.setEnqueueAt(System.currentTimeMillis());

        if (!dispatchQueue.offer(job)) {
            return Result.fail("分配队列已满");
        }

        return Result.success("已加入分配队列");
    }
}