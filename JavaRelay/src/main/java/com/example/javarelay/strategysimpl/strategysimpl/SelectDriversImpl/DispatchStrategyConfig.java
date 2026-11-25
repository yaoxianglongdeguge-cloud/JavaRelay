package com.example.javarelay.strategysimpl.strategysimpl.SelectDriversImpl;

import com.example.javarelay.strategysapi.SelectDriverStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 注册三个统一分配策略。
 *
 * <p>加新策略只需在这里补一个 {@code @Bean}，并在 {@code DispatchMetricPreset} 里加一组权重。
 */
@Configuration
public class DispatchStrategyConfig {

    @Bean
    public SelectDriverStrategy dispatchStrategy1() {
        return new BatchDispatchStrategy("default:1");
    }

    @Bean
    public SelectDriverStrategy dispatchStrategy2() {
        return new BatchDispatchStrategy("default:2");
    }

    @Bean
    public SelectDriverStrategy dispatchStrategy3() {
        return new BatchDispatchStrategy("default:3");
    }
}