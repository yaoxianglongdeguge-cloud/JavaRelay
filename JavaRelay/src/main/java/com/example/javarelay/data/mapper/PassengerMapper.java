package com.example.javarelay.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.javarelay.data.entity.Passenger;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PassengerMapper extends BaseMapper<Passenger> {

    @Select("select * from passengers where phone = #{phone}")
    public Passenger selectByPhone(@Param("phone") String phone);
}
