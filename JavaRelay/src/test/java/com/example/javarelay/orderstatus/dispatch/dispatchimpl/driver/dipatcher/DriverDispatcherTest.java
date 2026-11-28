package com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.dipatcher;

import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.metric.DriverMetric;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.metric.impl.DistanceMetric;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.metric.impl.IdleMetric;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.pojo.DriverCandidate;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.pojo.DriverMatchJob;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.pojo.DriverMatchResult;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.DriverMatchConstants;
import com.example.javarelay.orderstatus.dispatch.dispatchimpl.driver.mqtask.messagecells.DriverMatchTimeoutMessage;
import com.example.javarelay.orderstatus.dispatch.mqtask.messagecells.DispatchMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DriverDispatcherTest {

    private final DriverDispatcher dispatcher = new DriverDispatcher();

    private static DriverMatchJob order(long orderId, double lng, double lat, DriverMetric metric) {
        DriverMatchJob job = new DriverMatchJob();
        job.setOrderId(orderId);
        job.setPassengerId(orderId);
        job.setStartLng(lng);
        job.setStartLat(lat);
        job.setMetric(metric);
        return job;
    }

    private static DriverCandidate driver(long driverId, double lng, double lat, long idleMillis) {
        DriverCandidate c = new DriverCandidate();
        c.setDriverId(driverId);
        c.setLng(lng);
        c.setLat(lat);
        c.setIdleMillis(idleMillis);
        return c;
    }

    @Test
    @DisplayName("weight matrix")
    void weightMatrix() {

        DistanceMetric distanceMetric = new DistanceMetric();
        distanceMetric.setMaxDistance(1000);

        IdleMetric idleMetric = new IdleMetric();
        idleMetric.setCapMillis(1000);

        List<DriverMatchJob> orders = List.of(
                order(1, 120.15, 30.28, distanceMetric),
                order(2, 120.20, 30.30, idleMetric));

        List<DriverCandidate> drivers = List.of(
                driver(101, 120.15, 30.28, 500),
                driver(102, 120.15, 30.30, 2000),
                driver(103, 121.00, 31.00, 100));

        double[][] w = dispatcher.weights(orders, drivers);

        assertEquals(2, w.length);
        assertEquals(3, w[0].length);

        assertEquals(1.0, w[0][0], 1e-9);
        assertEquals(0.0, w[0][1], 1e-9);
        assertEquals(0.0, w[0][2], 1e-9);

        assertEquals(0.5, w[1][0], 1e-9);
        assertEquals(1.0, w[1][1], 1e-9);
        assertEquals(0.1, w[1][2], 1e-9);
    }

    @Test
    @DisplayName("empty input")
    void emptyInput() {
        assertEquals(0, dispatcher.weights(List.of(), List.of()).length);
    }


    @Test
    @DisplayName("assign publishes DRIVER done message")
    void assign() {

        DriverDispatcher task = new DriverDispatcher();
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        task.eventPublisher = publisher;

        DriverMatchJob job = new DriverMatchJob();
        job.setOrderId(1L);
        job.setPassengerId(2L);

        DriverCandidate driver = new DriverCandidate();
        driver.setDriverId(101L);

        DriverMatchResult result = new DriverMatchResult();
        result.setJob(job);
        result.setDriver(driver);
        result.setWeight(0.9);

        task.assign(result);

        ArgumentCaptor<DispatchMessage> captor = ArgumentCaptor.forClass(DispatchMessage.class);
        verify(publisher).publishEvent(captor.capture());

        DispatchMessage message = captor.getValue();

        assertTrue(message.getMsg().isDone());
        assertEquals(DispatchMessage.Step.DRIVER, message.getMsg().getStep());
        assertEquals(1L, message.getMsg().getOrderId());
        assertEquals(2L, message.getMsg().getPassengerId());
        assertEquals(101L, message.getMsg().getData().getDriverId());
    }

    @Test
    @DisplayName("超时的 job 不再回队，按未匹配处理")
    void dropTimeout() {

        DriverDispatcher task = new DriverDispatcher();
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        task.eventPublisher = publisher;

        long now = System.currentTimeMillis();

        DriverMatchJob fresh = new DriverMatchJob();
        fresh.setOrderId(1L);
        fresh.setEnqueueAt(now);

        DriverMatchJob expired = new DriverMatchJob();
        expired.setOrderId(2L);
        expired.setEnqueueAt(now - DriverMatchConstants.MATCH_DEADLINE_MS - 1);

        List<DriverMatchJob> alive = task.dropTimeout(List.of(fresh, expired));

        assertEquals(1, alive.size());
        assertEquals(1L, alive.get(0).getOrderId());

        ArgumentCaptor<DriverMatchTimeoutMessage> captor =
                ArgumentCaptor.forClass(DriverMatchTimeoutMessage.class);
        verify(publisher).publishEvent(captor.capture());

        assertEquals(2L, captor.getValue().getMsg().getOrderId());
    }
}