package com.example.javarelay.userinform.infra.websocket;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.infra.redis.rediscommon.RedisPefix;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WsPublisher {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    public void push(Integer userType, Long userId, String payload) {

        WsPushMessage message = new WsPushMessage(userType, userId, payload);

        try {
            Long receivers = stringRedisTemplate.convertAndSend(RedisPefix.WEBSOCKET_CHANNEL, JSON.toJSONString(message));
            if (receivers == null || receivers == 0) {
                log.debug("WS 推送无订阅者: userType={}, userId={}", userType, userId);
            }
        } catch (Exception e) {
            log.error("WS 推送发布失败: userType={}, userId={}", userType, userId, e);
        }
    }
}