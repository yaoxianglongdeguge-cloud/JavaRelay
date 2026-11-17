package com.example.javarelay.statustransition.service.Impl;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.data.entity.ServiceArea;
import com.example.javarelay.data.mapper.ServiceAreaMapper;
import com.example.javarelay.dto.Result;
import com.example.javarelay.rediscommon.RedisPefix;
import com.example.javarelay.statustransition.constants.Constants;
import com.example.javarelay.statustransition.dto.PassengerDto;
import com.example.javarelay.statustransition.service.ServeAreaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ServeAreaServiceImpl implements ServeAreaService {

    private static int nextScan = 0 ;

    @Autowired
    static ServiceAreaMapper staticServiceAreaMapper;

    @Autowired
    ServiceAreaMapper serviceAreaMapper;

    @Autowired
    static StringRedisTemplate stringRedisTemplate;

    static {
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
                String jsonArea = stringRedisTemplate.opsForValue().get(RedisPefix.SERVICEAREA_PREFIX + s.getCityName());
                ServiceArea area = JSON.parseObject(jsonArea, ServiceArea.class);

                if(area == null){
                    int a = 0;
                }else if(!area.equals(s))
                {
                    stringRedisTemplate.delete(RedisPefix.SERVICEAREA_PREFIX + s.getCityName());

                }else{
                    continue;
                }

                stringRedisTemplate.opsForValue().set(RedisPefix.SERVICEAREA_PREFIX + s.getCityName(),s.toString());

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

        stringRedisTemplate.opsForValue().set(RedisPefix.SERVICEAREA_PREFIX + serviceArea.getCityName()
                ,serviceArea.toString());

        return Result.success("修改成功");
    }

    @Override
    public Result judgeServeArea(PassengerDto passengerDto) {
        return null;
    }
}
