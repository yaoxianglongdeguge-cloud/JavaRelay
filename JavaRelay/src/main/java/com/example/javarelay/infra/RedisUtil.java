package com.example.javarelay.infra;


import com.example.javarelay.rediscommon.RedisPefix;
import com.example.javarelay.statustransition.statusenum.DriverStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RedisUtil {


    @Autowired
    public static StringRedisTemplate redisTemplate;

    private static final DefaultRedisScript<Long> LockScript;
    static {
        LockScript = new DefaultRedisScript<>();
        LockScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/try_lock.lua")));
        LockScript.setResultType(Long.class);
    }

    public static boolean tryLock(String lockKey,String uuid,Long timeout){

        Long result = redisTemplate.execute(
                LockScript,
                Collections.singletonList(lockKey),
                "uuid",
                "nil",
                uuid
        );

        Boolean ok = redisTemplate.expire(lockKey, timeout, TimeUnit.SECONDS);

        return result == 1L && Boolean.TRUE.equals(ok);

    }

    public static boolean tryunLock(String lockKey,String uuid){

        Long result = redisTemplate.execute(
                LockScript,
                Collections.singletonList(lockKey),
                "uuid",
                uuid,
                "nil"
        );

        return result == 1L;

    }



    public static boolean tryLockDriver(Long driverId) {
        String driverHashKey = RedisPefix.DRIVER_INFO + driverId;

        Long result = redisTemplate.execute(
                LockScript,
                Collections.singletonList(driverHashKey),
                "status",
                String.valueOf(DriverStatusEnum.LEISURE.getCode()),
                String.valueOf(DriverStatusEnum.RECEIVING.getCode())
        );

        return result != null && result == 1L;
    }

    public static boolean tryunLockDriver(Long driverId) {
        String driverHashKey = RedisPefix.DRIVER_INFO + driverId;

        Long result = redisTemplate.execute(
                LockScript,
                Collections.singletonList(driverHashKey),
                "status",
                String.valueOf(DriverStatusEnum.RECEIVING.getCode()),
                String.valueOf(DriverStatusEnum.LEISURE.getCode())
        );

        return result != null && result == 1L;
    }





}
