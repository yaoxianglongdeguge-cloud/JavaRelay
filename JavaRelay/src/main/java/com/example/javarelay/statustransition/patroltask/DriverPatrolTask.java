package com.example.javarelay.statustransition.patroltask;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.data.entity.DriverRedis;
import com.example.javarelay.data.mapper.DriverMapper;
import com.example.javarelay.rediscommon.RedisPefix;
import com.example.javarelay.statustransition.constants.Constants;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


@Slf4j
@Data
@Component
public class DriverPatrolTask implements Runnable{

    public final AtomicLong lastHeartbeat = new AtomicLong(System.currentTimeMillis());

    private static AtomicInteger nextScan = new AtomicInteger(0);

    private Future<?> patrolFuture = null;

    @Autowired
    DriverMapper driverMapper;
    @Autowired
    StringRedisTemplate stringRedisTemplate;


    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                scanDriver();
                lastHeartbeat.set(System.currentTimeMillis());
                Thread.sleep(Constants.DRIVER_PATROL_INTERVAL);
            } catch (InterruptedException e) {
                log.info("对账任务收到中断");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void scanDriver(){

        while (true) {

            List<DriverRedis> needInsert = null;
            try {
                needInsert = driverMapper.getDriverRedis(nextScan.get(), Constants.SERVE_DRIVERS_SCAN);
            } catch (Exception e) {
                log.info("司机扫描出错");
            }

            if(needInsert == null){
                nextScan.set(0);
                continue;
            }

            if (needInsert.size() < Constants.SERVE_DRIVERS_SCAN) {
                nextScan.set(0);
            } else {
                nextScan.addAndGet(needInsert.size());
            }

            for (DriverRedis s : needInsert) {
                String jsonDriver = stringRedisTemplate.opsForValue().get(RedisPefix.DRIVER_INFO + s.getId());
                DriverRedis driver = JSON.parseObject(jsonDriver, DriverRedis.class);

                if (driver == null) {
                    int a = 0;
                } else if (!driver.equals(s)) {
                    stringRedisTemplate.delete(RedisPefix.DRIVER_INFO + s.getId());

                } else {
                    continue;
                }

                stringRedisTemplate.opsForValue().set(RedisPefix.DRIVER_INFO + s.getId(), JSON.toJSONString(s));

            }
        }

    }
}
