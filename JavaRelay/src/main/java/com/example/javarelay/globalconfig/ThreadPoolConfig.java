package com.example.javarelay.globalconfig;

import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolConfig {

    private ExecutorService pool;

    @Bean
    public ExecutorService threadPool() {
        pool = Executors.newFixedThreadPool(4);
        return pool;
    }

    @PreDestroy
    public void closePool() {

        if (pool == null) {
            return;
        }

        pool.shutdown();

        try {
            if (!pool.awaitTermination(15, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}