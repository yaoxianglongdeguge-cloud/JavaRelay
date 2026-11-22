package com.example.javarelay.infra.mq;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.orderstatus.constants.Constants;
import com.example.javarelay.rootconstants.RootConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageGateway {

    @Autowired
    RocketMQTemplate rocketMQTemplate;

    @EventListener
    public boolean publish(MessageCell<?> cell) {

        if (cell == null || cell.getTopic() == null) {
            return false;
        }

        String topic = RootConstants.MQ_PREFIX + cell.getTopic();
        String body = JSON.toJSONString(cell);
        long delayMillis = delayOf(cell);

        Message<String> message = MessageBuilder.withPayload(body)
                .setHeader(RocketMQHeaders.KEYS, keyOf(cell))
                .build();

        try {
            SendResult result = delayMillis > 0
                    ? rocketMQTemplate.syncSend(topic, message, 3000, toDelayLevel(delayMillis))
                    : rocketMQTemplate.syncSend(topic, message, 3000);

            if (result == null || result.getSendStatus() == null) {
                log.error("消息发送返回异常 topic={}", topic);
                return false;
            }
            return true;

        } catch (Exception e) {
            log.error("消息发送失败 topic={} body={}", topic, body, e);
            return false;
        }
    }

    private long delayOf(MessageCell<?> cell) {
        if (cell.getExecuteTime() <= 0) {
            return 0;
        }
        return Math.max(0, cell.getExecuteTime() - System.currentTimeMillis());
    }

    private String keyOf(MessageCell<?> cell) {
        return cell.getTopic() + ":" + cell.getExecuteTime();
    }

    private int toDelayLevel(long delayMillis) {
        long[] levels = {1_000, 5_000, 10_000, 30_000, 60_000, 120_000,
                180_000, 240_000, 300_000, 360_000, 420_000, 480_000,
                540_000, 600_000, 1_200_000, 1_800_000, 3_600_000, 7_200_000};
        for (int i = 0; i < levels.length; i++) {
            if (levels[i] >= delayMillis) {
                return i + 1;
            }
        }
        return 18;
    }
}