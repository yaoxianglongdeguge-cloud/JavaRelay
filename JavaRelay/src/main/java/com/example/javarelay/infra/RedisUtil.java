package com.example.javarelay.infra;

import com.example.javarelay.statustransition.constants.RedisPefix;
import com.example.javarelay.statustransition.statusenum.DriverStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.Collections;

public class RedisUtil {

    @Autowired
    private static StringRedisTemplate redisTemplate;

    private static final DefaultRedisScript<Long> tryLockDriverScript;

    static {
        tryLockDriverScript = new DefaultRedisScript<>();
        tryLockDriverScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/try_lock_driver.lua")));
        tryLockDriverScript.setResultType(Long.class);
    }


    public static boolean tryLockDriver(Long driverId) {
        String driverHashKey = RedisPefix.DRIVER_INFO + driverId;

        Long result = redisTemplate.execute(
                tryLockDriverScript,
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
                tryLockDriverScript,
                Collections.singletonList(driverHashKey),
                "status",
                String.valueOf(DriverStatusEnum.RECEIVING.getCode()),
                String.valueOf(DriverStatusEnum.LEISURE.getCode())
        );

        return result != null && result == 1L;
    }





}
