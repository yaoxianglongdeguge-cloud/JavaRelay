package com.example.javarelay;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.example.javarelay.data.mapper")
public class JavaRelayApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaRelayApplication.class, args);
    }

}
