package com.example.javarelay.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.javarelay.data.entity.OrderDetail;
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
            "WHERE order_id = #{orderId} AND estimated_price IS NULL " +
            "AND (driver_id IS NULL OR driver_id = #{driverId})" +
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

    @Select("select acprice_type from order_detail where order_id = #{orderId}")
    public String getActualPriceType(@Param("orderId") Long orderId);


    /**
     * 重派补偿：把 order_detail 退回"只有起终点"的状态。
     */
    @Update("UPDATE order_detail SET driver_id = NULL, estimated_price = NULL, " +
            "route_snapshot = #{routeSnapshot}, update_time = NOW() " +
            "WHERE order_id = #{orderId} AND actual_price IS NULL")
    public int resetForRedispatch(@Param("orderId") Long orderId,
                                  @Param("routeSnapshot") String routeSnapshot);
    @Select("SELECT * FROM order_detail WHERE order_id = #{orderId}")
    OrderDetail selectByOrderId(@Param("orderId") Long orderId);

}
