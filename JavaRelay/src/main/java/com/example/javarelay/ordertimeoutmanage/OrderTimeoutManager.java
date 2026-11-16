package com.example.javarelay.ordertimeoutmanage;

import com.example.javarelay.constant.Constants;
import com.example.javarelay.dto.Result;
import com.example.javarelay.entity.Order;
import com.example.javarelay.exception.StatusChangeException;
import com.example.javarelay.mapper.DriverMapper;
import com.example.javarelay.mapper.OrderDetailMapper;
import com.example.javarelay.mapper.OrderMapper;
import com.example.javarelay.mapper.OrderStatusLogMapper;
import com.example.javarelay.service.OrderService;
import com.example.javarelay.service.PassengerOrderService;
import com.example.javarelay.status.DriverStatusEnum;
import com.example.javarelay.status.OrderOperatorEnum;
import com.example.javarelay.status.OrderStatusEnum;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Slf4j
@Component
public class OrderTimeoutManager {

    @Autowired
    OrderMapper orderMapper;

    @Autowired
    PassengerOrderService passengerOrderService;

    @Autowired
    OrderService orderService;

    private final DelayQueue<OrderTimeoutTask> delayQueue = new DelayQueue<>();

    ThreadPoolExecutor businessExecutor = new ThreadPoolExecutor(
            4, 8, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(2000),
            r -> new Thread(r, "timeout-business-worker"),
            new ThreadPoolExecutor.CallerRunsPolicy() // 核心：背压策略！
    );

    public void addTimeoutTask(Long orderId) {
        delayQueue.put(new OrderTimeoutTask(orderId, Constants.ORDER_TIMEOUT_TIME));
        log.info("订单 {} 加入超时监控队列，15秒后触发", orderId);
    }


    @PostConstruct
    public void startConsumer() {
        new Thread(() -> {
            log.info("超时监控线程启动...");
            while (true) {
                try {
                    OrderTimeoutTask task = delayQueue.take();
                    businessExecutor.execute(() -> {

                        try {
                            handleTimeout(task.getOrderId());
                        } catch (Exception e) {
                            log.error("处理订单超时异常, orderId: {}", task.getOrderId(), e);
                        }
                    });

                } catch (InterruptedException e) {
                    log.warn("超时监控线程被中断");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "timeout-consumer").start();
    }


    private void handleTimeout(Long orderId) {

        log.info("订单 {} 已超时，开始执行回滚...", orderId);


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

                passengerOrderService.ReDispatchOrder(orderId,passengerId);
                log.info("已重派单");

            } catch (StatusChangeException e) {

                log.info("订单已被取消或接单");
            } catch (Exception e){
                log.info("系统出问题");
            }

        }



    }
}
