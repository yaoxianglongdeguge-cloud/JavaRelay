package com.example.javarelay.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.javarelay.data.entity.Driver;
import com.example.javarelay.pojo.DriverRedis;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DriverMapper extends BaseMapper<Driver> {

    @Update("UPDATE drivers SET status = 1, update_time = NOW() WHERE id = #{driverId} AND status = 0")
    int occupy(@Param("driverId") Long driverId);

    @Update("UPDATE drivers SET status = 0, update_time = NOW() WHERE id = #{driverId} AND status = 1")
    int release(@Param("driverId") Long driverId);

    @Select("select id ,status from drivers where id >= #{nextScan} limit #{num}")
    public List<DriverRedis> getDriverRedis(@Param("nextScan") int nextScan,@Param("num") int num);
}