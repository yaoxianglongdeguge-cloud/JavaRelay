package com.example.javarelay.orderstatus.node;

import com.example.javarelay.orderstatus.entity.Order;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderNodeMapper {

    /**
     * 节点推进的串行点：锁订单行，并顺带判定是否已冻结。
     * 锁在 orders 行上，但它是"节点推进"的机制，不是订单查询。
     */
    @Select("SELECT * FROM orders WHERE id = #{orderId} FOR UPDATE")
    Order lockForAdvance(@Param("orderId") Long orderId);

    @Insert("INSERT INTO order_node (order_id, node_key) VALUES (#{orderId}, #{nodeKey})")
    int insertNode(@Param("orderId") Long orderId, @Param("nodeKey") String nodeKey);

    @Select("SELECT node_key FROM order_node WHERE order_id = #{orderId}")
    List<String> selectKeys(@Param("orderId") Long orderId);

    @Select("SELECT COUNT(*) FROM order_node WHERE order_id = #{orderId} AND node_key = #{nodeKey}")
    int count(@Param("orderId") Long orderId, @Param("nodeKey") String nodeKey);

    @Delete("<script>" +
            "DELETE FROM order_node WHERE order_id = #{orderId} AND node_key IN " +
            "<foreach collection='keys' item='k' open='(' separator=',' close=')'>#{k}</foreach>" +
            "</script>")
    int deleteMany(@Param("orderId") Long orderId, @Param("keys") List<String> keys);
}