package com.example.javarelay.infra.redis;


import com.example.javarelay.infra.redis.rediscommon.RedisPefix;
import com.example.javarelay.orderstatus.constants.statusenum.DriverStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class RedisUtil {


    @Autowired
    public StringRedisTemplate redisTemplate;

    private static final DefaultRedisScript<Long> driverLockScript;
    private static final DefaultRedisScript<Long> lockScript;
    private static final DefaultRedisScript<Long> unLockScript;
    static {
        driverLockScript = new DefaultRedisScript<>();
        driverLockScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/try_lock_driver.lua")));
        driverLockScript.setResultType(Long.class);

        lockScript = new DefaultRedisScript<>();
        lockScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/try_lock.lua")));
        lockScript.setResultType(Long.class);

        unLockScript = new DefaultRedisScript<>();
        unLockScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/try_unlock.lua")));
        unLockScript.setResultType(Long.class);
    }

    public boolean tryLock(String lockKey,String uuid,Long timeout){

        Long result = redisTemplate.execute(
                lockScript,
                Collections.singletonList(lockKey),
                uuid,
                String.valueOf(timeout)
        );


        return result == 1L;

    }

    public boolean tryunLock(String lockKey,String uuid){

        Long result = redisTemplate.execute(
                unLockScript,
                Collections.singletonList(lockKey),
                uuid
        );

        return result == 1L;

    }



    public boolean tryLockDriver(Long driverId) {
        String driverHashKey = RedisPefix.DRIVER_INFO + driverId;

        Long result = redisTemplate.execute(
                driverLockScript,
                Collections.singletonList(driverHashKey),
                String.valueOf(DriverStatusEnum.LEISURE.getCode()),
                String.valueOf(DriverStatusEnum.RECEIVING.getCode())
        );

        return result != null && result == 1L;
    }

    public boolean tryunLockDriver(Long driverId) {
        String driverHashKey = RedisPefix.DRIVER_INFO + driverId;

        Long result = redisTemplate.execute(
                driverLockScript,
                Collections.singletonList(driverHashKey),
                String.valueOf(DriverStatusEnum.RECEIVING.getCode()),
                String.valueOf(DriverStatusEnum.LEISURE.getCode())
        );

        return result != null && result == 1L;
    }





}
