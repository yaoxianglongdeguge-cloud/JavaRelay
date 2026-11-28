package com.example.javarelay.orderstatus.dispatch.mqtask.messagehandlersimpl;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.dto.Result;
import com.example.javarelay.orderstatus.dispatch.mqtask.messagecells.DispatchMessage;
import com.example.javarelay.orderstatus.dispatch.service.DispatchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RocketMQMessageListener(topic = "jr_dispatch", consumerGroup = "jr_dispatch_group")
public class DispatchHandlerImpl implements RocketMQListener<String> {

    @Autowired
    DispatchService dispatchService;

    @Override
    public void onMessage(String body) {

        DispatchMessage cell = JSON.parseObject(body, DispatchMessage.class);

        if (cell == null || cell.getMsg() == null) {
            log.warn("派单消息格式错误: {}", body);
            return;
        }

        Long orderId = cell.getMsg().getOrderId();
        Long passengerId = cell.getMsg().getPassengerId();

        DispatchMessage.Step step = cell.getMsg().getStep();

        // step 必填：缺了说明消息格式不对，直接丢弃
        if (step == null) {
            log.warn("派单消息缺少 step，丢弃: {}", body);
            return;
        }

        Result result;

        try {
            result = execute(step, orderId, passengerId);
        } catch (Exception e) {
            log.error("派单步骤异常 orderId={} step={}", orderId, step, e);
            if (step == DispatchMessage.Step.PATH) {
                throw e;                        // 交给 MQ 重试
            }
            return;
        }

        if (result != null && result.getCode() == 200) {
            return;
        }

        String reason = result == null ? "null" : result.getMessage();

        switch (step) {
            case DRIVER -> log.warn("派单入队失败 orderId={} reason={}", orderId, reason);
            case PATH -> throw new RuntimeException("路线步骤失败: " + reason);
            case PASSENGER -> log.info("乘客校验未通过，服务内部已处理 orderId={} reason={}", orderId, reason);
        }
    }

    private Result execute(DispatchMessage.Step step, Long orderId, Long passengerId) {

        return switch (step) {
            case PASSENGER -> dispatchService.dispatch(orderId, passengerId);
            case DRIVER -> dispatchService.reDispatch(orderId, passengerId);
            case PATH -> dispatchService.assignPath(orderId, passengerId);
        };
    }
}
