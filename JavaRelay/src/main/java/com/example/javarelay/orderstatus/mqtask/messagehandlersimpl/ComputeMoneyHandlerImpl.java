package com.example.javarelay.orderstatus.mqtask.messagehandlersimpl;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.orderstatus.mapper.OrderDetailMapper;
import com.example.javarelay.orderstatus.service.OrderService;
import com.example.javarelay.dto.Result;
import com.example.javarelay.orderstatus.mqtask.messagecells.ComputeMoneyMessage;
import com.example.javarelay.strategysapi.ComputeMoneyStrategy;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RocketMQMessageListener(topic = "jr_computemoney", consumerGroup = "jr_computemoney_group")
public class ComputeMoneyHandlerImpl implements RocketMQListener<String> {

    @Autowired
    List<ComputeMoneyStrategy> computeMoneyStrategyList;

    Map<String, ComputeMoneyStrategy> computeMoneyStrategys = new HashMap<>();

    @Autowired
    OrderDetailMapper orderDetailMapper;

    @Autowired
    OrderService orderService;

    @PostConstruct
    public void init() {
        for (ComputeMoneyStrategy s : computeMoneyStrategyList) {
            computeMoneyStrategys.put(s.getType(), s);
        }
    }

    @Override
    public void onMessage(String body) {

        ComputeMoneyMessage cell = JSON.parseObject(body, ComputeMoneyMessage.class);

        if (cell == null || cell.getMsg() == null) {
            log.warn("金额计算消息格式错误: {}", body);
            return;
        }

        Long orderId = cell.getMsg().getOrderId();

        String type;
        try {
            type = orderDetailMapper.getActualPriceType(orderId);
        } catch (Exception e) {
            log.error("查询计费类型失败 orderId={}", orderId, e);
            return;
        }

        if (type == null) {
            log.warn("订单 {} 没有计费类型，跳过金额计算", orderId);
            return;
        }

        ComputeMoneyStrategy strategy = computeMoneyStrategys.get(type);
        if (strategy == null) {
            log.warn("订单 {} 找不到计费策略 type={}", orderId, type);
            return;
        }

        Result result = strategy.ComputeStrategy(orderId);
        BigDecimal money = result == null ? null : (BigDecimal) result.getData();

        if (money == null) {
            log.warn("订单 {} 金额计算返回空", orderId);
            return;
        }
        orderService.settleOrder(orderId, money);
    }
}