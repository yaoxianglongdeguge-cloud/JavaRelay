package com.example.javarelay.statustransition.messagetask.messagehandlersimpl;

import com.example.javarelay.statustransition.config.MessageTaskConfig;
import com.example.javarelay.dto.Result;
import com.example.javarelay.data.mapper.OrderDetailMapper;
import com.example.javarelay.infra.mq.MessageCell;
import com.example.javarelay.statustransition.messagetask.messagecells.ComputeMoneyMessage;
import com.example.javarelay.infra.mq.MessageHandler;
import com.example.javarelay.statustransition.strategys.ComputeMoneyStrategy;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ComputeMoneyHandlerImpl implements MessageHandler {

    public String topic = MessageTaskConfig.MESSAGE_COMPUTEMONEY_TOPIC;

    @Autowired
    List<ComputeMoneyStrategy> computeMoneyStrategyList;
    Map<String, ComputeMoneyStrategy> computeMoneyStrategys = new HashMap<>();

    @Autowired
    OrderDetailMapper orderDetailMapper;

    ThreadPoolExecutor computeMoneyExecutor = new ThreadPoolExecutor(
            4, 8, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(2000),
            r -> new Thread(r, "compute-money-worker"),
            new ThreadPoolExecutor.AbortPolicy()
    );

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

        try{

            computeMoneyExecutor.execute(()->handle(messageCell));

        }catch(RejectedExecutionException e){
            log.info("金额计算线程池已满");
        }

        return true;
    }

    public void handle(MessageCell messageCell) {

        ComputeMoneyMessage computeMoneyMessage = (ComputeMoneyMessage) messageCell;
        ComputeMoneyMessage.msg msg = (ComputeMoneyMessage.msg) computeMoneyMessage.getMsg();
        Long orderId = msg.getOrderId();
        String type = null;
        try {
            type = orderDetailMapper.getActualPriceType(orderId);
        } catch (Exception e) {
            log.info("查找金额计算策略失败");
        }

        if (type == null) {
            log.info("查找金额计算策略失败");
        }

        ComputeMoneyStrategy computeMoneyStrategy = computeMoneyStrategys.get(type);
        Result result = computeMoneyStrategy.ComputeStrategy(orderId);
        BigDecimal money = (BigDecimal) result.getData();

        if (money == null) {
            log.info("金额计算失败");
            return;
        }

        try {
            orderDetailMapper.updateActualPrice(orderId, money);
        } catch (Exception e) {
            log.info("金额插入失败");
        }


    }
}
