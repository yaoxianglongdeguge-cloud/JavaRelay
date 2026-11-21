package com.example.javarelay.infra.redis.rediscommon;

import jdk.jfr.DataAmount;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RedisLogicTimeout<T> {
   T data;
    // 逻辑过期时间戳（LocalDateTime / long毫秒）
   LocalDateTime expireTime;
}
