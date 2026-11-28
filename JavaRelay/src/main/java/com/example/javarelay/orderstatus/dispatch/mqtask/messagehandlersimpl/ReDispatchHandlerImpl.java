package com.example.javarelay.orderstatus.dispatch.mqtask.messagehandlersimpl;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.dto.Result;
import com.example.javarelay.orderstatus.dispatch.mqtask.messagecells.ReDispatchMessage;
import com.example.javarelay.orderstatus.service.PassengerOperateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RocketMQMessageListener(topic = "jr_redispatch", consumerGroup = "jr_redispatch_group")
public class ReDispatchHandlerImpl implements RocketMQListener<String> {

    @Autowired
    PassengerOperateService passengerOperateService;

    @Override
    public void onMessage(String body) {

        ReDispatchMessage cell = JSON.parseObject(body, ReDispatchMessage.class);

        if (cell == null || cell.getMsg() == null) {
            log.warn("重派单消息格式错误: {}", body);
            return;
        }

        Long orderId = cell.getMsg().getOrderId();
        Long passengerId = cell.getMsg().getPassengerId();

        Result result = passengerOperateService.ReDispatchOrder(orderId, passengerId);

        if (result == null || result.getCode() != 200) {
            log.warn("重派入队失败 orderId={} reason={}", orderId,
                    result == null ? "null" : result.getMessage());
        }
    }
}