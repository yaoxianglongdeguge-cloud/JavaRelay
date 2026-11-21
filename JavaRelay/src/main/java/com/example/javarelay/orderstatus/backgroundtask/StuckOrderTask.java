package com.example.javarelay.orderstatus.backgroundtask;

import com.example.javarelay.data.entity.Order;
import com.example.javarelay.data.mapper.OrderMapper;
import com.example.javarelay.redis.RedisUtil;
import com.example.javarelay.orderstatus.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StuckOrderTask {

    @Autowired
    OrderMapper orderMapper;
    @Autowired
    OrderService orderService;
    @Autowired
    RedisUtil redisUtil;

    @Scheduled(fixedDelay = 60_000)
    public void sweep() {
        for (Order o : orderMapper.selectStuckOrders(5)) {
            log.warn("清理超时未推进订单 {}", o.getId());
            orderService.cancelAfterDispatchFail(o.getId(), o.getPassengerId(),
                    o.getStatus(), o.getDriverId(), "订单超时未推进");
            if (o.getDriverId() != null) {
                redisUtil.tryunLockDriver(o.getDriverId());
            }
        }
    }
}