package com.example.javarelay.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.javarelay.data.entity.OrderDetail;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

public interface OrderDetailMapper extends BaseMapper<OrderDetail> {

    @Insert("INSERT INTO order_detail (order_id, passenger_id, route_snapshot, estimated_price, actual_price, " +
            "acprice_type, esprice_type, driver_type, passenger_type, path_type, data_stage, " +
            "create_time, update_time) " +
            "VALUES (#{orderId}, #{passengerId}, #{routeSnapshot}, NULL, NULL, #{acpriceType}, " +
            "#{espriceType}, #{driverType}, #{passengerType}, #{pathType}, 0, NOW(), NOW())")
    int insertDetail(OrderDetail detail);

    @Update("UPDATE order_detail od JOIN orders o ON o.id = od.order_id " +
            "SET od.route_snapshot = #{routeSnapshot}, od.estimated_price = #{estimatedPrice}, " +
            "od.data_stage = 1, od.update_time = NOW() " +
            "WHERE od.order_id = #{orderId} AND od.data_stage = 0 " +
            "AND o.status = 1 AND o.driver_id = #{driverId}")
    int saveRouteAndPrice(@Param("orderId") Long orderId,
                          @Param("driverId") Long driverId,
                          @Param("estimatedPrice") BigDecimal estimatedPrice,
                          @Param("routeSnapshot") String routeSnapshot);

    @Update("UPDATE order_detail SET actual_price = #{actualPrice}, data_stage = NULL, update_time = NOW() " +
            "WHERE order_id = #{orderId} AND data_stage = 1 AND actual_price IS NULL")
    int settle(@Param("orderId") Long orderId, @Param("actualPrice") BigDecimal actualPrice);

    @Update("UPDATE order_detail SET estimated_price = NULL, route_snapshot = #{routeSnapshot}, " +
            "data_stage = 0, update_time = NOW() " +
            "WHERE order_id = #{orderId} AND data_stage IS NOT NULL AND actual_price IS NULL")
    int resetForRedispatch(@Param("orderId") Long orderId, @Param("routeSnapshot") String routeSnapshot);

    @Update("UPDATE order_detail SET data_stage = NULL, update_time = NOW() " +
            "WHERE order_id = #{orderId} AND data_stage IS NOT NULL AND actual_price IS NULL")
    int freeze(@Param("orderId") Long orderId);

    @Select("SELECT acprice_type FROM order_detail WHERE order_id = #{orderId}")
    String getActualPriceType(@Param("orderId") Long orderId);

    @Select("SELECT * FROM order_detail WHERE order_id = #{orderId}")
    OrderDetail selectByOrderId(@Param("orderId") Long orderId);
}