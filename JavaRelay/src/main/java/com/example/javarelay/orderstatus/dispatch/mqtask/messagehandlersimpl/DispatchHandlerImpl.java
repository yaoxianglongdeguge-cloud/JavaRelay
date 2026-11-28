package com.example.javarelay.orderstatus.dispatch.mqtask.messagehandlersimpl;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.dto.Result;
import com.example.javarelay.orderstatus.dispatch.dto.DispatchData;
import com.example.javarelay.orderstatus.dispatch.mqtask.messagecells.DispatchMessage;
import com.example.javarelay.orderstatus.dispatch.service.DispatchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(topic = "jr_dispatch", consumerGroup = "jr_dispatch_group")
public class DispatchHandlerImpl implements RocketMQListener<String> {

    @Autowired
    DispatchService dispatchService;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @Override
    public void onMessage(String body) {

        DispatchMessage cell = JSON.parseObject(body, DispatchMessage.class);

        if (cell == null || cell.getMsg() == null) {
            log.warn("派单消息格式错误: {}", body);
            return;
        }

        DispatchMessage.msg msg = cell.getMsg();

        if (msg.isDone()) {
            handleAfter(msg);
        } else {
            handleBefore(msg);
        }
    }

    // 处理前：只算不写库
    private void handleBefore(DispatchMessage.msg msg) {

        Long orderId = msg.getOrderId();
        Long passengerId = msg.getPassengerId();

        DispatchData data;

        try {
            data = dispatchService.prepare(msg.getStep(), orderId, passengerId);
        } catch (Exception e) {
            log.error("环节准备失败 orderId={} step={}", orderId, msg.getStep(), e);
            return;
        }

        if (data == null) {
            log.info("环节未完成，等待后续 orderId={} step={}", orderId, msg.getStep());
            return;
        }

        eventPublisher.publishEvent(
                new DispatchMessage(orderId, passengerId, msg.getStep(), true, data));
    }

    // 处理后：只写库
    private void handleAfter(DispatchMessage.msg msg) {

        Long orderId = msg.getOrderId();
        Long passengerId = msg.getPassengerId();

        Result result;

        try {
            result = dispatchService.commit(msg.getStep(), orderId, passengerId, msg.getData());
        } catch (Exception e) {
            log.error("环节落库失败 orderId={} step={}", orderId, msg.getStep(), e);
            return;
        }

        if (result == null || result.getCode() != 200) {
            log.warn("环节落库未通过 orderId={} step={} reason={}", orderId, msg.getStep(),
                    result == null ? "null" : result.getMessage());
            return;
        }

        log.info("环节完成 orderId={} step={} result={}", orderId, msg.getStep(), result.getMessage());

        DispatchMessage.Step next = msg.getStep().next();

        if (next == null) {
            return;
        }

        eventPublisher.publishEvent(new DispatchMessage(orderId, passengerId, next));
    }
}