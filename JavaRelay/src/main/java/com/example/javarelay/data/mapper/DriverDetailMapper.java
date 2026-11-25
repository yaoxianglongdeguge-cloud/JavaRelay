package com.example.javarelay.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.javarelay.data.entity.Passenger;
import org.apache.ibatis.annotations.Mapper;

import com.example.javarelay.data.entity.DriverDetail;
import com.example.javarelay.pojo.DriverAttr;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface DriverDetailMapper extends BaseMapper<DriverDetail> {

    /**
     * 司机慢变属性。以 driver_detail 为准（右连接），没有 detail 的司机不会被返回。
     * 游标分页必须 ORDER BY，否则翻页可能重复或遗漏。
     */
    @Select("select dd.driver_id as id, dd.score as score " +
            "from drivers d " +
            "right join driver_detail dd on dd.driver_id = d.id " +
            "where dd.driver_id >= #{nextScan} " +
            "order by dd.driver_id " +
            "limit #{num}")
    public List<DriverAttr> getDriverAttrs(@Param("nextScan") int nextScan, @Param("num") int num);

    @Select("select * from driver_detail where phone = #{phone}")
    public DriverDetail selectByPhone(@Param("phone") String phone);
}
