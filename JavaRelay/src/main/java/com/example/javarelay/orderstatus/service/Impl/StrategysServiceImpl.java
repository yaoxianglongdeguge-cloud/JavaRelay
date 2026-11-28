package com.example.javarelay.orderstatus.service.Impl;

import com.example.javarelay.orderstatus.entity.OrderDetail;
import com.example.javarelay.orderstatus.mapper.OrderDetailMapper;
import com.example.javarelay.dto.Result;
import com.example.javarelay.orderstatus.pojo.RouteInfo;
import com.example.javarelay.strategysimpl.infocollect.InfoCollector;
import com.example.javarelay.orderstatus.mqtask.messagecells.ComputeMoneyMessage;
import com.example.javarelay.strategysapi.PreComputeStrategy;
import com.example.javarelay.strategysapi.SelectDriverStrategy;
import com.example.javarelay.strategysapi.SelectPassengerStrategy;
import com.example.javarelay.strategysapi.SelectPathStrategy;
import com.example.javarelay.orderstatus.service.StrategysService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class StrategysServiceImpl implements StrategysService {

    Map<Long,InfoCollector> infoCollectors = new ConcurrentHashMap<>();

    @Autowired
    List<SelectDriverStrategy> selectDriverStrategyList;
    Map<String, SelectDriverStrategy> selectDriverStrategys = new HashMap<>();

    @Autowired
    List<SelectPassengerStrategy> selectPassengerStrategyList;
    Map<String, SelectPassengerStrategy> selectPassengerStrategys = new HashMap<>();

    @Autowired
    List<SelectPathStrategy> selectPathStrategyList;
    Map<String, SelectPathStrategy> selectPathStrategys = new HashMap<>();

    @Autowired
    List<PreComputeStrategy> preComputeStrategyList;
    Map<String, PreComputeStrategy> preComputeStrategys = new HashMap<>();

    @Autowired
    OrderDetailMapper orderDetailMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @PostConstruct
    public void init(){
        for(SelectDriverStrategy s:selectDriverStrategyList){
            selectDriverStrategys.put(s.getType(),s);
        }

        for(SelectPassengerStrategy s:selectPassengerStrategyList){
            selectPassengerStrategys.put(s.getType(),s);
        }

        for(SelectPathStrategy s:selectPathStrategyList){
            selectPathStrategys.put(s.getType(),s);
        }

        for(PreComputeStrategy s:preComputeStrategyList){
            preComputeStrategys.put(s.getType(),s);
        }


    }

    public Result putInfoCollector(Long orderId){

        OrderDetail orderDetail = null;
        try{
            orderDetail = orderDetailMapper.selectByOrderId(orderId);
        }catch (Exception e){
            return Result.fail("插入失败");
        }

        if(orderDetail == null){
            return Result.fail("插入失败");
        }

        InfoCollector infoCollector = new InfoCollector();
        infoCollector.setOrderId(orderId);
        infoCollector.setSelectDriverStrategy(orderDetail.getDriverType());

        infoCollector.setSelectPassengerStrategy(orderDetail.getPassengerType());

        infoCollector.setPrecomputeMoneyType(orderDetail.getEspriceType());

        infoCollector.setComputeMoneyType(orderDetail.getAcpriceType());

        infoCollector.setSelectPathStrategyType(orderDetail.getPathType());

        infoCollector.setPassengerId(orderDetail.getPassengerId());

        RouteInfo routeInfo = orderDetail.deserializeRouteInfo();

        infoCollector.setStartLat(routeInfo.getStartLat());
        infoCollector.setStartLng(routeInfo.getStartLng());
        infoCollector.setEndLat(routeInfo.getEndLat());
        infoCollector.setEndLng(routeInfo.getEndLng());

        infoCollector.setStartAddress(routeInfo.getStartAddress());
        infoCollector.setEndAddress(routeInfo.getEndAddress());

        infoCollectors.put(orderId,infoCollector);

        return Result.success("ok");
    }

    public Result deleteInfoCollector(Long orderId) {
        if (infoCollectors.get(orderId) == null) {
            return Result.success("删除成功");
        }

        infoCollectors.remove(orderId);
        if (infoCollectors.get(orderId) == null) {
            return Result.success("删除成功");
        }

        return Result.fail("删除失败");

    }

    public InfoCollector getInfoCollector(Long orderId) {
        InfoCollector c = infoCollectors.get(orderId);
        if (c != null) return c;
        putInfoCollector(orderId);
        return infoCollectors.get(orderId);
    }



    @Override
    public Result findPassengerByStrategy(Long orderId) {
        InfoCollector infoCollector = getInfoCollector(orderId);
        if (infoCollector == null) return Result.fail("上下文构建失败");
        String type = infoCollector.getSelectPassengerStrategy();
        return selectPassengerStrategys.get(type).findPassengerStrategy(infoCollector);
    }

    @Override
    public Result findDriverByStrategy(Long orderId) {

        InfoCollector infoCollector = getInfoCollector(orderId);
        if (infoCollector == null) return Result.fail("上下文构建失败");
        String type = infoCollector.getSelectDriverStrategy();
        SelectDriverStrategy strategy = selectDriverStrategys.get(type);

        // 兼容历史订单：driver_type 为空或不在已注册策略里时，回退到 default
        if (strategy == null) {
            strategy = selectDriverStrategys.get("default");
            if (type != null && !type.isEmpty()) {
                log.warn("未找到选车策略 {}，回退到 default", type);
            }
        }

        if (strategy == null) {
            return Result.fail("没有可用的选车策略");
        }

        return strategy.findDriverStrategy(infoCollector);
    }

    @Override
    public Result findPathByStrategy(Long orderId) {
        InfoCollector infoCollector = getInfoCollector(orderId);
        if (infoCollector == null) return Result.fail("上下文构建失败");
        String type = infoCollector.getSelectPathStrategyType();
        deleteInfoCollector(orderId);
        return selectPathStrategys.get(type).findPathStrategy(infoCollector);
    }

    @Override
    public Result preComputeMoney(Long orderId) {
        InfoCollector infoCollector = getInfoCollector(orderId);
        if (infoCollector == null) return Result.fail("上下文构建失败");
        String type = infoCollector.getPrecomputeMoneyType();
        return preComputeStrategys.get(type).PreComputeStrategy(infoCollector);
    }

    @Override
    public Result ComputeMoney(Long orderId) {

        ComputeMoneyMessage computeMoneyMessage = new ComputeMoneyMessage(orderId);

        eventPublisher.publishEvent(computeMoneyMessage);

        return null;
    }

}
