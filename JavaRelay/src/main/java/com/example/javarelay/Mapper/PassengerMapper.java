package com.example.javarelay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.javarelay.entity.Passenger;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PassengerMapper extends BaseMapper<Passenger> {
}
