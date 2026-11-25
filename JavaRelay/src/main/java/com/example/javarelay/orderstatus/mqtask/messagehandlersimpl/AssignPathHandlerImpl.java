package com.example.javarelay.orderstatus.mqtask.messagehandlersimpl;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.dto.Result;
import com.example.javarelay.orderstatus.mqtask.messagecells.AssignPathMessage;
import com.example.javarelay.orderstatus.service.DispatchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(topic = "jr_assignPath", consumerGroup = "jr_assignPath_group")
public class AssignPathHandlerImpl implements RocketMQListener<String> {

    @Autowired
    DispatchService dispatchService;

    @Override
    public void onMessage(String body) {

        AssignPathMessage cell = JSON.parseObject(body, AssignPathMessage.class);

        if (cell == null || cell.getMsg() == null) {
            log.warn("路线消息格式错误: {}", body);
            return;
        }

        Result result = dispatchService.AssignPath(
                cell.getMsg().getOrderId(), cell.getMsg().getPassengerId());

        if (result == null || result.getCode() != 200) {
            // 抛出异常让 MQ 重试（路线是可重入的，且失败不该静默）
            throw new RuntimeException("路线计算失败: " + (result == null ? "null" : result.getMessage()));
        }
    }
}