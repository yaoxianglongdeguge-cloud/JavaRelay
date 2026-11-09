package com.example.javarelay.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.javarelay.Entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT * FROM ride_order WHERE passenger_id = #{passengerId}")
    public Order selectByPassenger(Long passengerId);
}
