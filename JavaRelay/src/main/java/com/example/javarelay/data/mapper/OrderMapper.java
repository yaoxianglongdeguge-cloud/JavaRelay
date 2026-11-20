package com.example.javarelay.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.javarelay.data.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT * FROM orders WHERE passenger_id = #{passengerId}")
    public Order selectByPassenger(Long passengerId);

    @Update("<script>" +
            "UPDATE orders SET status = #{toStatus}, update_time = NOW() " +
            "<if test='driverId != null'>, driver_id = #{driverId} </if>" +
            "WHERE id = #{orderId} AND status = #{fromStatus}" +
            "</script>")
    public int updateStatusWithOptimisticLock(@Param("orderId") Long orderId,
                                       @Param("fromStatus") Integer fromStatus,
                                       @Param("toStatus") Integer toStatus,
                                       @Param("driverId") Long driverId);


    @Update("<script>" +
            "UPDATE orders SET status = #{toStatus}, update_time = NOW() " +
            "<if test='driverId != null'>, driver_id = #{driverId} </if>" +
            "WHERE id = #{orderId} AND status IN " +
            "<foreach collection='fromStatus' item='status' open='(' separator=',' close=')'>" +
            "#{status}" +
            "</foreach>" +
            "</script>")
    public int updateCancelWithOptimisticLock(@Param("orderId") Long orderId,
                                              @Param("fromStatus") Integer[] fromStatus,
                                              @Param("toStatus") Integer toStatus,
                                              @Param("driverId") Long driverId);

    @Update("<script>" +
            "UPDATE orders SET active_flag = #{toFlag}, update_time = NOW() " +
            "WHERE id = #{orderId} AND active_flag = #{fromFlag}" +
            "</script>")
    public int updateActiveFlag(@Param("orderId") Long orderId,
                                @Param("fromFlag") Integer fromFlag,
                                @Param("toFlag") Integer toFlag
                                             );


    @Select("SELECT * FROM orders WHERE passenger_id = #{passengerId} " +
            "AND active_flag = 1 ORDER BY create_time DESC LIMIT 1")
    public Order passengerSelectUnFinshed(Long passengerId);

    @Select("SELECT * FROM orders WHERE driver_id = #{driverId} " +
            "AND active_flag = 1 ORDER BY create_time DESC LIMIT 1")
    public Order driverSelectUnFinshed(Long driverId);

    //把超过一定时间没有有效推进的订单清理掉
    @Select("SELECT * FROM orders WHERE active_flag = 1 AND status IN (0, 1) " +
            "AND update_time < DATE_SUB(NOW(), INTERVAL #{minutes} MINUTE) LIMIT 100")
    List<Order> selectStuckOrders(@Param("minutes") int minutes);

    //把没有超过一定时间但是没有被接单的订单拿出来，进行派单或者超时等待接单
    @Select("SELECT * FROM orders " +
            "WHERE active_flag = 1 AND status IN (0, 1) " +
            "AND update_time > DATE_SUB(NOW(), INTERVAL #{hours} HOUR) " +
            "ORDER BY id LIMIT #{offset}, #{limit}")
    List<Order> selectRecoverable(@Param("offset") int offset,
                                  @Param("limit") int limit,
                                  @Param("hours") int hours);

}
