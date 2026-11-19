package com.example.javarelay.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.javarelay.data.entity.Driver;
import com.example.javarelay.data.entity.DriverRedis;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DriverMapper extends BaseMapper<Driver> {

    @Update("UPDATE drivers SET status = #{newStatus}, update_time = NOW() " +
                   "WHERE id = #{driverId} AND status = #{oldStatus}")
    int updateStatusWithOptimisticLock(@Param("driverId") Long driverId,
                                       @Param("oldStatus") Integer oldStatus,
                                       @Param("newStatus") Integer newStatus);

    @Select("select id ,status from drivers where id >= #{nextScan} limit #{num}")
    public List<DriverRedis> getDriverRedis(@Param("nextScan") int nextScan,@Param("num") int num);
}
