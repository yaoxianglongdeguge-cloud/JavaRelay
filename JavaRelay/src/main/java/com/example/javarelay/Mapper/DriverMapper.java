package com.example.javarelay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.javarelay.entity.Driver;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DriverMapper extends BaseMapper<Driver> {

    @Update("UPDATE driver SET status = #{newStatus}, update_time = NOW() " +
                   "WHERE id = #{driverId} AND status = #{oldStatus}")
    int updateStatusWithOptimisticLock(@Param("driverId") Long driverId,
                                       @Param("oldStatus") Integer oldStatus,
                                       @Param("newStatus") Integer newStatus);
}
