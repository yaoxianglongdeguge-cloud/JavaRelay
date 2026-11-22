package com.example.javarelay.userinform.infra.websocket;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class WsPushConfig {

    @Bean
    public RedisMessageListenerContainer wsPushContainer(RedisConnectionFactory factory,
                                                         MyWebSocketHandler handler) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);

        container.addMessageListener((message, pattern) -> {
            try {
                String body = new String(message.getBody(), StandardCharsets.UTF_8);
                WsPushMessage push = JSON.parseObject(body, WsPushMessage.class);
                if (push == null || push.getUserId() == null || push.getUserType() == null) {
                    return;
                }
                handler.sendLocal(push.getUserType(), push.getUserId(), push.getPayload());
            } catch (Exception e) {
                log.error("WS 推送消息处理失败", e);
            }
        }, new ChannelTopic(RedisPrefix.CHANNEL));

        return container;
    }
}