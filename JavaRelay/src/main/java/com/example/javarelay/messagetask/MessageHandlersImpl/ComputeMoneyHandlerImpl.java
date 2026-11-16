package com.example.javarelay.messagetask.MessageHandlersImpl;

import com.example.javarelay.constant.Constants;
import com.example.javarelay.dto.Result;
import com.example.javarelay.entity.OrderDetail;
import com.example.javarelay.infocollect.InfoCollector;
import com.example.javarelay.mapper.OrderDetailMapper;
import com.example.javarelay.messagetask.MessageCell;
import com.example.javarelay.messagetask.MessageCells.ComputeMoneyMessage;
import com.example.javarelay.messagetask.MessageHandler;
import com.example.javarelay.strategys.ComputeMoneyStrategy;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ComputeMoneyHandlerImpl implements MessageHandler {

    public String topic = Constants.MESSAGE_COMPUTEMONEY_TOPIC;

    @Autowired
    List<ComputeMoneyStrategy> computeMoneyStrategyList;
    Map<String, ComputeMoneyStrategy> computeMoneyStrategys = new HashMap<>();

    @Autowired
    OrderDetailMapper orderDetailMapper;

    @PostConstruct
    public void init(){
        for(ComputeMoneyStrategy s:computeMoneyStrategyList){
            computeMoneyStrategys.put(s.getType(),s);
        }
    }

    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public Boolean handler(MessageCell messageCell) {

        ComputeMoneyMessage computeMoneyMessage = (ComputeMoneyMessage) messageCell;
        ComputeMoneyMessage.msg msg = (ComputeMoneyMessage.msg)computeMoneyMessage.getMsg();
        InfoCollector infoCollector = msg.getInfoCollector();
        ComputeMoneyStrategy computeMoneyStrategy = computeMoneyStrategys.get(infoCollector.getComputeMoneyType());
        Result result = computeMoneyStrategy.ComputeStrategy(infoCollector);
        BigDecimal money = (BigDecimal) result.getData();
        Long orderId = infoCollector.getOrderId();

        if(money == null){
            return false;
        }

        try{
            orderDetailMapper.updateActualPrice(orderId,money);
        }catch (Exception e){
            log.info("金额插入失败");
        }


        return true;
    }
}
