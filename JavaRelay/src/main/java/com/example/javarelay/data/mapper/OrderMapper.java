package com.example.javarelay.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.javarelay.data.entity.Order;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Insert("INSERT INTO orders (passenger_id, status, driver_id, active_flag, create_time, update_time) " +
            "VALUES (#{passengerId}, #{status}, NULL, #{activeFlag}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertOrder(Order order);

    @Update("UPDATE orders SET status = 1, driver_id = #{driverId}, update_time = NOW() " +
            "WHERE id = #{orderId} AND status = 0 AND driver_id IS NULL AND active_flag = 1")
    int dispatch(@Param("orderId") Long orderId, @Param("driverId") Long driverId);

    @Update("UPDATE orders SET status = 2, update_time = NOW() " +
            "WHERE id = #{orderId} AND status = 1 AND driver_id = #{driverId}")
    int receive(@Param("orderId") Long orderId, @Param("driverId") Long driverId);

    @Update("UPDATE orders SET status = 4, update_time = NOW() " +
            "WHERE id = #{orderId} AND status = 2 AND driver_id = #{driverId}")
    int arrive(@Param("orderId") Long orderId, @Param("driverId") Long driverId);

    @Update("UPDATE orders SET status = 7, active_flag = NULL, update_time = NOW() " +
            "WHERE id = #{orderId} AND status = 4 AND active_flag = 1")
    int finish(@Param("orderId") Long orderId);

    @Update("UPDATE orders SET status = 0, driver_id = NULL, update_time = NOW() " +
            "WHERE id = #{orderId} AND status = 1 AND driver_id = #{driverId}")
    int rollbackToWaiting(@Param("orderId") Long orderId, @Param("driverId") Long driverId);

    @Update("UPDATE orders SET status = 6, active_flag = NULL, update_time = NOW() " +
            "WHERE id = #{orderId} AND status IN (0, 1, 2) AND active_flag = 1")
    int cancel(@Param("orderId") Long orderId);

    @Select("SELECT * FROM orders WHERE passenger_id = #{passengerId} " +
            "AND active_flag = 1 ORDER BY create_time DESC LIMIT 1")
    Order passengerSelectUnFinshed(Long passengerId);

    @Select("SELECT * FROM orders WHERE driver_id = #{driverId} " +
            "AND active_flag = 1 ORDER BY create_time DESC LIMIT 1")
    Order driverSelectUnFinshed(Long driverId);

    @Select("SELECT * FROM orders WHERE active_flag = 1 AND status IN (0, 1) " +
            "AND update_time < DATE_SUB(NOW(), INTERVAL #{minutes} MINUTE) LIMIT 100")
    List<Order> selectStuckOrders(@Param("minutes") int minutes);
}
