package com.example.javarelay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.javarelay.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT * FROM ride_order WHERE passenger_id = #{passengerId}")
    public Order selectByPassenger(Long passengerId);

    @Update("<script>" +
            "UPDATE ride_order SET status = #{toStatus}, update_time = NOW() " +
            "<if test='driverId != null'>, driver_id = #{driverId} </if>" +
            "WHERE id = #{orderId} AND status = #{fromStatus}" +
            "</script>")
    int updateStatusWithOptimisticLock(@Param("orderId") Long orderId,
                                       @Param("fromStatus") Integer fromStatus,
                                       @Param("toStatus") Integer toStatus,
                                       @Param("driverId") Long driverId);
}
