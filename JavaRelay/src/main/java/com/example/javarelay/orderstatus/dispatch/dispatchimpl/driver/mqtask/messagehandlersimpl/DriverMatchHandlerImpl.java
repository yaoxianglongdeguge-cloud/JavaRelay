package com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.mqtask.messagehandlersimpl;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.dipatcher.DriverMatchQueue;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.metric.DriverMetric;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.mqtask.messagecells.DriverMatchMessage;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.pojo.DriverMatchJob;
import com.example.javarelay.orderstatus.entity.OrderDetail;
import com.example.javarelay.orderstatus.mapper.OrderDetailMapper;
import com.example.javarelay.orderstatus.pojo.RouteInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RocketMQMessageListener(topic = "jr_match_driver", consumerGroup = "jr_match_driver_group")
public class DriverMatchHandlerImpl implements RocketMQListener<String> {

    @Autowired
    OrderDetailMapper orderDetailMapper;

    @Autowired
    DriverMatchQueue driverMatchQueue;

    @Autowired
    List<DriverMetric> driverMetrics;

    @Override
    public void onMessage(String body) {

        DriverMatchMessage cell = JSON.parseObject(body, DriverMatchMessage.class);

        if (cell == null || cell.getMsg() == null) {
            log.warn("派司机消息格式错误: {}", body);
            return;
        }

        Long orderId = cell.getMsg().getOrderId();
        Long passengerId = cell.getMsg().getPassengerId();

        OrderDetail detail = orderDetailMapper.selectByOrderId(orderId);

        RouteInfo route = detail == null ? null : detail.deserializeRouteInfo();

        if (route == null || route.getStartLng() == null || route.getStartLat() == null) {
            log.warn("订单 {} 缺少起点，无法派司机", orderId);
            return;
        }

        DriverMetric metric = pickMetric(orderId, detail);

        DriverMatchJob job = new DriverMatchJob();
        job.setOrderId(orderId);
        job.setPassengerId(passengerId);
        job.setMetric(metric);
        job.setStartLng(route.getStartLng().doubleValue());
        job.setStartLat(route.getStartLat().doubleValue());
        job.setEnqueueAt(System.currentTimeMillis());

        if (!driverMatchQueue.offer(job)) {
            // 入队失败不能吞掉，抛出去交给 MQ 重试
            throw new RuntimeException("派司机任务入队失败 orderId=" + orderId);
        }

        log.info("派司机任务入队 orderId={} metric={} 队列={}", orderId, metric.type(), driverMatchQueue.size());
    }

    // 乘客下单时选的 type 决定用哪个指标，没匹配上就退到 default
    private DriverMetric pickMetric(Long orderId, OrderDetail detail) {

        String type = detail == null ? null : detail.getDriverType();

        for (DriverMetric metric : driverMetrics) {
            if (metric.type().equals(type)) {
                return metric;
            }
        }
        for (DriverMetric metric : driverMetrics) {
            if ("default".equals(metric.type())) {
                log.warn("订单 {} 没有匹配的指标 {}，退到 default", orderId, type);
                return metric;
            }
        }

        throw new RuntimeException("没有可用的派司机指标 orderId=" + orderId);
    }
}