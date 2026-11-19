package com.example.javarelay.statustransition.messagetask.messagehandlersimpl;

import com.example.javarelay.data.entity.Order;
import com.example.javarelay.data.mapper.OrderMapper;
import com.example.javarelay.infra.messagetask.MessageCell;
import com.example.javarelay.infra.messagetask.MessageHandler;
import com.example.javarelay.statustransition.config.MessageTaskConfig;
import com.example.javarelay.statustransition.exception.StatusChangeException;
import com.example.javarelay.statustransition.service.OrderService;
import com.example.javarelay.statustransition.service.PassengerOperateService;
import com.example.javarelay.statustransition.statusenum.DriverStatusEnum;
import com.example.javarelay.statustransition.statusenum.OrderOperatorEnum;
import com.example.javarelay.statustransition.statusenum.OrderStatusEnum;
import com.example.javarelay.statustransition.messagetask.messagecells.OrderTimeoutMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class OrderReceiveDelayHandler implements MessageHandler {

    String topic = MessageTaskConfig.ORDER_TIMEOUT_TOPIC;

    @Autowired
    OrderMapper orderMapper;

    @Autowired
    OrderService orderService;

    @Autowired
    PassengerOperateService passengerOperateService;

    @Override
    public String getTopic() {
        return this.topic;
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

    ThreadPoolExecutor computeMoneyExecutor = new ThreadPoolExecutor(
            4, 8, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(2000),
            r -> new Thread(r, "compute-money-worker"),
            new ThreadPoolExecutor.AbortPolicy()
    );

    private void handle(MessageCell messageCell){
        OrderTimeoutMessage orderTimeoutMessage = (OrderTimeoutMessage) messageCell;
        OrderTimeoutMessage.msg msg = (OrderTimeoutMessage.msg) orderTimeoutMessage.getMsg();
        Long orderId = msg.getOrderId();
        handleTimeout(orderId);

    }

    private void handleTimeout(Long orderId) {


        Order order = null;
        try{
            order = orderMapper.selectById(orderId);
        }catch (Exception e){
            log.info("系统问题");
        }

        Integer status = order.getStatus();
        Long passengerId = order.getPassengerId();
        Long driverId = order.getDriverId();

        if(status == OrderStatusEnum.DISPATCHED.getCode()) {

            try {
                orderService.changeOrderStatus(orderId, passengerId,
                        OrderStatusEnum.DISPATCHED.getCode(),
                        OrderStatusEnum.WAITING_DISPATCH.getCode(),
                        OrderOperatorEnum.SYSTEM.getCode(),

                        driverId,
                        DriverStatusEnum.RECEIVING.getCode(),
                        DriverStatusEnum.LEISURE.getCode());

                passengerOperateService.ReDispatchOrder(orderId,passengerId);
                log.info("已重派单");

            } catch (StatusChangeException e) {

                log.info("订单已被取消或接单");
            } catch (Exception e){
                log.info("系统出问题");
            }

        }



    }
}
