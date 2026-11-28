package com.example.javarelay.orderstatus.dispatch.service;

import com.example.javarelay.dto.Result;
import com.example.javarelay.orderstatus.dispatch.dto.DispatchData;
import com.example.javarelay.orderstatus.dispatch.mqtask.messagecells.DispatchMessage;

public interface DispatchService {

    // 只算不写库；返回 null 表示本环节还没完成（等匹配任务）
    DispatchData prepare(DispatchMessage.Step step, Long orderId, Long passengerId);

    // 只写库
    Result commit(DispatchMessage.Step step, Long orderId, Long passengerId, DispatchData data);
}