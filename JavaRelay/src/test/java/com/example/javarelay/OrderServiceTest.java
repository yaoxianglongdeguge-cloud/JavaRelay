package com.example.javarelay;

import com.example.javarelay.dto.PassengerDto;
import com.example.javarelay.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

@SpringBootTest // 启动完整的 Spring 容器
public class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Test
    public void testCreateOrder() {
        // 1. 准备测试数据 DTO
        PassengerDto dto = new PassengerDto();
        dto.setPassengerId(1L);
        dto.setStartLng(new BigDecimal("120.153576"));
        dto.setStartLat(new BigDecimal("30.287459"));
        dto.setEndLng(new BigDecimal("120.163576"));
        dto.setEndLat(new BigDecimal("30.297459"));

        // 2. 调用 Service 进行下单
        try {
            Boolean result = orderService.createAndInsertOrder(dto);
            System.out.println("下单结果: " + result);
        } catch (Exception e) {
            System.out.println("测试报错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}