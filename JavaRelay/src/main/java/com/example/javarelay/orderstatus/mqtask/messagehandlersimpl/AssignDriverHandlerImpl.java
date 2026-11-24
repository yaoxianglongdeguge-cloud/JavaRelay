package com.example.javarelay.orderstatus.mqtask.messagehandlersimpl;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.dto.Result;
import com.example.javarelay.orderstatus.mqtask.messagecells.AssignDriverMessage;
import com.example.javarelay.orderstatus.mqtask.messagecells.ReDispatchMessage;
import com.example.javarelay.orderstatus.service.PassengerOperateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(topic = "jr_assignDriver", consumerGroup = "jr_assignDriver_group")
public class AssignDriverHandlerImpl implements RocketMQListener<String> {

    /** 派单失败时的重试间隔（毫秒） */
    private static final long RETRY_DELAY_MS = 500L;

    @Autowired
    PassengerOperateService passengerOperateService;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @Override
    public void onMessage(String body) {

        AssignDriverMessage cell = JSON.parseObject(body, AssignDriverMessage.class);

        if (cell == null || cell.getMsg() == null) {
            log.warn("派单消息格式错误: {}", body);
            return;
        }

        Long orderId = cell.getMsg().getOrderId();
        Long passengerId = cell.getMsg().getPassengerId();

        Result result = passengerOperateService.AssignDriver(orderId, passengerId);

        if (result == null || result.getCode() != 200) {

            log.info("派单未成功，转入重试链路 orderId={} reason={}",
                    orderId, result == null ? "null" : result.getMessage());

            // 复用既有重试链路（内含 60 秒 deadline 判断）
            ReDispatchMessage retry = new ReDispatchMessage(orderId, passengerId);
            retry.setExecuteTime(System.currentTimeMillis() + RETRY_DELAY_MS);
            eventPublisher.publishEvent(retry);
        }
    }
}