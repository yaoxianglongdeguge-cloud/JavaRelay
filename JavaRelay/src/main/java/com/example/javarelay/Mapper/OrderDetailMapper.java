package com.example.javarelay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.javarelay.entity.OrderDetail;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

public interface OrderDetailMapper extends BaseMapper<OrderDetail> {

    @Update("<script>" +
            "UPDATE order_detail " +
            "<set> " +
            "  <if test='driverId != null'> driver_id = #{driverId}, </if>" +
            "  <if test='estimatedPrice != null'> estimated_price = #{estimatedPrice}, </if>" +
            "  <if test='routeSnapshot != null'> route_snapshot = #{routeSnapshot}, </if>" +
            "  update_time = NOW() " +
            "</set> " +
            "WHERE order_id = #{orderId} AND route_snapshot IS NULL" +
            "</script>")
    public int initOrderDetail(
            @Param("orderId") Long orderId,
            @Param("driverId") Long driverId,
            @Param("estimatedPrice") BigDecimal estimatedPrice,
            @Param("routeSnapshot") String routeSnapshot);

    @Update("UPDATE order_detail SET actual_price = #{actual_price} where order_id = #{orderId} and " +
            "actual_price is null")
    public int updateActualPrice(
            @Param("orderId") Long orderId,
            @Param("actual_price") BigDecimal actual_price);

    @Select("select acpricetype from order_detail where order_id = #{orderId}")
    public String getActualPriceType(@Param("orderId") Long orderId);

}
