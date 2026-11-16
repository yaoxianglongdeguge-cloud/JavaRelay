package com.example.javarelay.service.Impl;

import com.example.javarelay.constant.Constants;
import com.example.javarelay.dto.PassengerDto;
import com.example.javarelay.dto.Result;
import com.example.javarelay.entity.Passenger;
import com.example.javarelay.infocollect.InfoCollector;
import com.example.javarelay.messagetask.MessageCells.ComputeMoneyMessage;
import com.example.javarelay.messagetask.MessageDispatcher;
import com.example.javarelay.strategys.*;
import com.example.javarelay.service.StrategysService;
import jakarta.annotation.PostConstruct;
import org.apache.tomcat.util.bcel.Const;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StrategysServiceImpl implements StrategysService {

    @Autowired
    Map<Long,InfoCollector> infoCollectors = new HashMap<>();

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
    MessageDispatcher messageDispatcher;

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

    public Result putInfoCollector(Long orderId, PassengerDto passengerDto){

        InfoCollector infoCollector = new InfoCollector();
        infoCollector.setOrderId(orderId);
        infoCollector.setSelectDriverStrategy(passengerDto.getSelectDriverStrategy());
        infoCollector.setDriverStrategyMark(1);

        infoCollector.setSelectPassengerStrategy(Constants.PASSENGER_SELECT_TYPE);
        infoCollector.setPassengerStrategyMark(1);

        infoCollector.setPrecomputeMoneyType(Constants.PRECOMPUTE_MONEY_TYPE);
        infoCollector.setPrecomMark(1);

        infoCollector.setComputeMoneyType(passengerDto.getComputeMoneyType());
        infoCollector.setComputeMark(1);

        infoCollector.setSelectPathStrategyType(Constants.PATH_SELECT_TYPE);
        infoCollector.setPathStrategyMark(1);

        infoCollector.setPassengerId(passengerDto.getPassengerId());
        infoCollector.setStartLat(passengerDto.getStartLat());
        infoCollector.setStartLng(passengerDto.getStartLng());
        infoCollector.setEndLat(passengerDto.getEndLat());
        infoCollector.setEndLng(passengerDto.getEndLng());

        infoCollector.setStartAddress(passengerDto.getStartAddress());
        infoCollector.setEndAddress(passengerDto.getEndAddress());


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

    public InfoCollector getInfoCollector(Long orderId){
       return infoCollectors.get(orderId);
    }



    @Override
    public Result findPassengerByStrategy(Long orderId) {
        InfoCollector infoCollector = infoCollectors.get(orderId);
        String type = infoCollector.getSelectPassengerStrategy();
        return selectPassengerStrategys.get(type).findPassengerStrategy(infoCollector);
    }

    @Override
    public Result findDriverByStrategy(Long orderId) {

        InfoCollector infoCollector = infoCollectors.get(orderId);
        String type = infoCollector.getSelectDriverStrategy();
        return selectDriverStrategys.get(type).findDriverStrategy(infoCollector);
    }

    @Override
    public Result findPathByStrategy(Long orderId) {
        return null;
    }

    @Override
    public Result preComputeMoney(Long orderId) {
        return null;
    }

    @Override
    public Result ComputeMoney(Long orderId) {

        InfoCollector infoCollector = infoCollectors.get(orderId);

        if(infoCollector.getComputeMark() == null){
            return Result.fail("没有设置计价策略");
        }

        ComputeMoneyMessage computeMoneyMessage = new ComputeMoneyMessage(infoCollector);
        computeMoneyMessage.setTopic(Constants.MESSAGE_COMPUTEMONEY_TOPIC);

        if(!messageDispatcher.putMessage(computeMoneyMessage)){
            return Result.fail("信息投递失败");
        }

        return null;
    }

}
