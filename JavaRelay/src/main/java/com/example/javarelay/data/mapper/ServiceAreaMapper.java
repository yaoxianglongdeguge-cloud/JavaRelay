package com.example.javarelay.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.javarelay.data.entity.ServiceArea;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ServiceAreaMapper extends BaseMapper<ServiceArea> {

    @Select("select * from service_area where id >= #{nextScan} limit #{num};")
    public List<ServiceArea> selectServeArea(@Param("nextScan") int nextScan,@Param("num") int num);

    @Select("select * from service_area where city_name = #{city}")
    public ServiceArea selectByCity(@Param("city") String city);

}
