package com.example.javarelay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.javarelay.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT * FROM ride_order WHERE passenger_id = #{passengerId}")
    public Order selectByPassenger(Long passengerId);
}
