package com.example.javarelay.orderstatus.backgroundtask;

import com.example.javarelay.data.entity.Order;
import com.example.javarelay.data.mapper.OrderMapper;
import com.example.javarelay.orderstatus.constants.Constants;
import com.example.javarelay.orderstatus.mqtask.messagecells.OrderTimeoutMessage;
import com.example.javarelay.orderstatus.mqtask.messagecells.ReDispatchMessage;
import com.example.javarelay.orderstatus.constants.statusenum.OrderStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class DispatchRecover implements ApplicationRunner {

    @Autowired
    OrderMapper orderMapper;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @Override
    public void run(ApplicationArguments args) {

        long now = System.currentTimeMillis();
        int pending = 0, dispatched = 0, offset = 0;

        while (true) {

            List<Order> batch = orderMapper.selectRecoverable(offset, Constants.DISPATCH_RECOVER_BATCH,
                    Constants.DISPATCH_RECOVER_MAX_AGE_HOURS);
            if (batch == null || batch.isEmpty()) {
                break;
            }

            for (Order o : batch) {

                if (Objects.equals(o.getStatus(), OrderStatusEnum.WAITING_DISPATCH.getCode())) {
                    // 待派单：重新投递，继续找司机
                    eventPublisher.publishEvent(new ReDispatchMessage(o.getId(), o.getPassengerId()));
                    pending++;

                } else if (Objects.equals(o.getStatus(), OrderStatusEnum.DISPATCHED.getCode())) {
                    // 已派单：重启前投的 15 秒超时消息丢了，按剩余时间补投
                    long ageMs = now - o.getUpdateTime()
                            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    long remainMs = Math.max(0, Constants.ORDER_TIMEOUT_TIME * 1000L - ageMs);

                    OrderTimeoutMessage msg = new OrderTimeoutMessage(o.getId(), 0);
                    msg.setExecuteTime(now + remainMs);   // 已超时则为 now，立即触发
                    eventPublisher.publishEvent(msg);
                    dispatched++;
                }
            }

            offset += batch.size();
            if (batch.size() < Constants.DISPATCH_RECOVER_BATCH) {
                break;
            }
        }

        log.info("消息恢复完成：待派单 {} 条，已派单超时监控 {} 条", pending, dispatched);
    }
}