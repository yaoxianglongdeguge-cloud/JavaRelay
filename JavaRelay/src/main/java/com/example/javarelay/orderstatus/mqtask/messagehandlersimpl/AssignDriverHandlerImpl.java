package com.example.javarelay.orderstatus.mqtask.messagehandlersimpl;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.dto.Result;
import com.example.javarelay.orderstatus.dispatch.EnqueueRetryPolicy;
import com.example.javarelay.orderstatus.mqtask.messagecells.AssignDriverMessage;
import com.example.javarelay.orderstatus.service.DispatchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 派单消息消费者：只负责把订单交给选车策略。
 *
 * <p>结果语义唯一：200 = 已入队；非 200 = 没入队，交给 {@link EnqueueRetryPolicy} 决定后续。
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "jr_assignDriver", consumerGroup = "jr_assignDriver_group")
public class AssignDriverHandlerImpl implements RocketMQListener<String> {

    @Autowired
    DispatchService dispatchService;

    @Autowired
    EnqueueRetryPolicy enqueueRetryPolicy;

    @Override
    public void onMessage(String body) {

        AssignDriverMessage cell = JSON.parseObject(body, AssignDriverMessage.class);

        if (cell == null || cell.getMsg() == null) {
            log.warn("派单消息格式错误: {}", body);
            return;
        }

        Long orderId = cell.getMsg().getOrderId();
        Long passengerId = cell.getMsg().getPassengerId();

        Result result = dispatchService.AssignDriver(orderId, passengerId);

        if (result == null || result.getCode() != 200) {
            enqueueRetryPolicy.onEnqueueFailed(orderId, passengerId, 0,
                    result == null ? "null" : result.getMessage());
        }
    }
}