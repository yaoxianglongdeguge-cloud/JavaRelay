package com.example.javarelay.orderstatus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.javarelay.orderstatus.entity.OrderDetail;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

public interface OrderDetailMapper extends BaseMapper<OrderDetail> {

    @Insert("INSERT INTO order_detail (order_id, passenger_id, route_snapshot, estimated_price, actual_price, " +
            "acprice_type, esprice_type, driver_type, passenger_type, path_type, create_time, update_time) " +
            "VALUES (#{orderId}, #{passengerId}, #{routeSnapshot}, NULL, NULL, #{acpriceType}, " +
            "#{espriceType}, #{driverType}, #{passengerType}, #{pathType}, NOW(), NOW())")
    int insertDetail(OrderDetail detail);

    @Update("UPDATE order_detail SET route_snapshot = #{routeSnapshot}, estimated_price = #{estimatedPrice}, " +
            "update_time = NOW() WHERE order_id = #{orderId}")
    int markRouteAndPrice(@Param("orderId") Long orderId,
                          @Param("estimatedPrice") BigDecimal estimatedPrice,
                          @Param("routeSnapshot") String routeSnapshot);

    @Update("UPDATE order_detail SET actual_price = #{actualPrice}, update_time = NOW() WHERE order_id = #{orderId}")
    int markSettled(@Param("orderId") Long orderId, @Param("actualPrice") BigDecimal actualPrice);

    @Update("UPDATE order_detail SET actual_price = NULL, update_time = NOW() WHERE order_id = #{orderId}")
    int clearActualPrice(@Param("orderId") Long orderId);

    @Select("SELECT acprice_type FROM order_detail WHERE order_id = #{orderId}")
    String getActualPriceType(@Param("orderId") Long orderId);

    @Select("SELECT * FROM order_detail WHERE order_id = #{orderId}")
    OrderDetail selectByOrderId(@Param("orderId") Long orderId);
}