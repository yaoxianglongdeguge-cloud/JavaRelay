package com.example.javarelay.statustransition.checkthread;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.data.entity.DriverRedis;
import com.example.javarelay.data.entity.ServiceArea;
import com.example.javarelay.data.mapper.DriverMapper;
import com.example.javarelay.rediscommon.RedisPefix;
import com.example.javarelay.statustransition.constants.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

@Slf4j
public class DriverCheck {

    @Autowired
    DriverMapper driverMapper;
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    private static int nextScan;

    static {

    }

    private void scanDriver(){

        while (true) {

            List<DriverRedis> needInsert = null;
            try {
                needInsert = driverMapper.getDriverRedis(nextScan, Constants.SERVE_DRIVERS_SCAN);
            } catch (Exception e) {
                log.info("司机扫描出错");
            }

            if(needInsert == null){
                nextScan = 0;
                continue;
            }

            if (needInsert.size() < Constants.SERVE_DRIVERS_SCAN) {
                nextScan = 0;
            } else {
                nextScan = nextScan + needInsert.size();
            }

            for (DriverRedis s : needInsert) {
                String jsonDriver = stringRedisTemplate.opsForValue().get(RedisPefix.DRIVER_INFO + s.getId());
                DriverRedis driver = JSON.parseObject(jsonDriver, DriverRedis.class);

                if (driver == null) {
                    int a = 0;
                } else if (!driver.equals(s)) {
                    stringRedisTemplate.delete(RedisPefix.SERVICEAREA_PREFIX + s.getId());

                } else {
                    continue;
                }

                stringRedisTemplate.opsForValue().set(RedisPefix.SERVICEAREA_PREFIX + s.getId(), JSON.toJSONString(s));

            }
        }

    }
}
