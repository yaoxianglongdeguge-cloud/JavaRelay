package com.example.javarelay.statustransition.service.Impl;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.data.entity.ServiceArea;
import com.example.javarelay.data.mapper.ServiceAreaMapper;
import com.example.javarelay.dto.Result;
import com.example.javarelay.infra.RedisUtil;
import com.example.javarelay.rediscommon.RedisLogicTimeout;
import com.example.javarelay.rediscommon.RedisPefix;
import com.example.javarelay.statustransition.constants.Constants;
import com.example.javarelay.statustransition.dto.PassengerDto;
import com.example.javarelay.statustransition.service.ServeAreaService;
import com.alibaba.fastjson2.TypeReference;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.valves.JsonAccessLogValve;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ServeAreaServiceImpl implements ServeAreaService {

    private static int nextScan = 0 ;

    @Autowired
    ServiceAreaMapper serviceAreaMapper;

    @Autowired
    ServiceAreaMapper staticServiceAreaMapper;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Resource
    private ExecutorService threadPool;

    @Autowired
    RedisUtil redisUtil;

    @PostConstruct
    public void warmUpServeArea() {
        while(nextScan != -1){

            List<ServiceArea> needInsert = null;
            try{
                needInsert = staticServiceAreaMapper.selectServeArea(nextScan, Constants.SERVE_AREA_SCAN);
            }catch (Exception e){
                log.info("服务范围扫描出错");
            }

            if(needInsert.size() < Constants.SERVE_AREA_SCAN){
                nextScan = -1;
            }else{
                nextScan = nextScan + needInsert.size();
            }

            for(ServiceArea s: needInsert){

                ServiceArea area = getServiceAreaRedis(s.getCityName());
                if (area == null) {
                    setServiceAreaRedis(s.getCityName(), s, 86400);
                    continue;
                }
                if(!area.equals(s))
                {
                    stringRedisTemplate.delete(RedisPefix.SERVICEAREA_PREFIX + s.getCityName());

                }else{
                    continue;
                }

                setServiceAreaRedis(s.getCityName(),s,86400);

            }
        }




    }

    @Override
    public Result changeServeArea(ServiceArea serviceArea) {

        if(serviceArea == null || serviceArea.getCityName() == null){
            return Result.fail("信息不全");
        }


        int rows = 0;
        try{
            rows = serviceAreaMapper.updateById(serviceArea);
        }catch (Exception e){
            return Result.fail("数据库出问题");
        }

        if(rows == 0){
            return Result.fail("数据库修改失败");
        }

        setServiceAreaRedis(serviceArea.getCityName(),serviceArea,86400);


        return Result.success("修改成功");
    }

    @Override
    public Result getServeArea(String city) {

        String dataServiceAreaStr = null;
        RedisLogicTimeout<ServiceArea> dataServiceArea = null;

        if(city == null || city.isEmpty()){
            return Result.fail("信息不全");
        }

        dataServiceAreaStr = stringRedisTemplate.opsForValue().get(RedisPefix.SERVICEAREA_PREFIX + city);

        if(dataServiceAreaStr != null && !dataServiceAreaStr.isEmpty()){

            dataServiceArea = JSON.parseObject(dataServiceAreaStr, new TypeReference<RedisLogicTimeout<ServiceArea>>() {});

            LocalDateTime logicExpireTime = dataServiceArea.getExpireTime();
            if(!LocalDateTime.now().isAfter(logicExpireTime)){
                return Result.success(dataServiceArea.getData());
            }
        }

        UUID uuid = UUID.randomUUID();
        String token = uuid.toString();
        boolean lock = redisUtil.tryLock(RedisPefix.SERVICEAREA_PREFIX + city,token,5L);
        if(lock){
            try{

                threadPool.submit(() -> reCreateCache(city));
            }finally {
                redisUtil.tryunLock(RedisPefix.SERVICEAREA_PREFIX + city,token);
            }
        }

        if(dataServiceAreaStr == null || dataServiceAreaStr.isEmpty()){
            return Result.fail("服务繁忙，请稍后");
        }

        LocalDateTime logicExpireTime = dataServiceArea.getExpireTime();
        if(!LocalDateTime.now().isAfter(logicExpireTime)){
            return Result.success((ServiceArea)dataServiceArea.getData());
        }

        return Result.success();
    }

    private void reCreateCache(String city){

        ServiceArea serviceArea = serviceAreaMapper.selectByCity(city);
        if(serviceArea == null){
            setServiceAreaRedis(city,null,3);
            stringRedisTemplate.expire(RedisPefix.SERVICEAREA_PREFIX + city, 1, TimeUnit.SECONDS);
        }else{
            setServiceAreaRedis(city,serviceArea,86400);
        }

    }

    public boolean setServiceAreaRedis(String city,Object data,long seconds){
        RedisLogicTimeout<Object> value = new RedisLogicTimeout<>();
        value.setData(data);
        value.setExpireTime(LocalDateTime.now().plusSeconds(seconds));
        stringRedisTemplate.opsForValue().set(RedisPefix.SERVICEAREA_PREFIX + city,
                JSON.toJSONString(value));

        return true;
    }

    public ServiceArea getServiceAreaRedis(String city) {
        String str = stringRedisTemplate.opsForValue().get(RedisPefix.SERVICEAREA_PREFIX + city);
        if (str == null || str.isEmpty()) {
            return null;
        }
        RedisLogicTimeout<ServiceArea> wrapper =
                JSON.parseObject(str, new TypeReference<RedisLogicTimeout<ServiceArea>>() {});
        return wrapper == null ? null : wrapper.getData();
    }

}
