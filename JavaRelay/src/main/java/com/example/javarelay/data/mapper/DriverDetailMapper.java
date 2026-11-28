package com.example.javarelay.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.javarelay.data.entity.Passenger;
import org.apache.ibatis.annotations.Mapper;

import com.example.javarelay.data.entity.DriverDetail;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface DriverDetailMapper extends BaseMapper<DriverDetail> {

    @Select("select * from driver_detail where phone = #{phone}")
    public DriverDetail selectByPhone(@Param("phone") String phone);
}
