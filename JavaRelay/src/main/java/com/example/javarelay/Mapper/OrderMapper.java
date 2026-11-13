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
    public int updateStatusWithOptimisticLock(@Param("orderId") Long orderId,
                                       @Param("fromStatus") Integer fromStatus,
                                       @Param("toStatus") Integer toStatus,
                                       @Param("driverId") Long driverId);

    @Update("<script>" +
            "UPDATE ride_order SET active_flag = #{toFlag}, update_time = NOW() " +
            "WHERE id = #{orderId} AND active_flag = #{fromFlag}" +
            "</script>")
    public int updateActiveFlag(@Param("orderId") Long orderId,
                                              @Param("fromStatus") Integer fromFlag,
                                              @Param("toStatus") Integer toFlag
                                             );


    @Select("SELECT * FROM ride_order WHERE passenger_id = #{passengerId} " +
            "AND active_flag == 1 ORDER BY create_time DESC LIMIT 1")
    public Order passengerSelectUnFinshed(Long passengerId);

    @Select("SELECT * FROM ride_order WHERE driver_id = #{driverId} " +
            "AND active_flag == 1 ORDER BY create_time DESC LIMIT 1")
    public Order driverSelectUnFinshed(Long driverId);

}
