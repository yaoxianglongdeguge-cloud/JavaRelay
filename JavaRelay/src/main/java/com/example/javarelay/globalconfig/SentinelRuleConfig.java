package com.example.javarelay.globalconfig;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class SentinelRuleConfig {

    @PostConstruct
    public void init() {

        System.setProperty("csp.sentinel.slotchain.builder",
                "com.alibaba.csp.sentinel.slots.HotParamSlotChainBuilder");

        ParamFlowRuleManager.loadRules(List.of(
                perUser("order:put",          1, 5),    // 5 秒 1 次
                perUser("order:cancel",       3, 10),   // 10 秒 3 次
                perUser("order:finish",       2, 10),   // 10 秒 2 次
                perUser("order:receive",      5, 1),    // 1 秒 5 次
                perUser("order:arrive",       2, 10),   // 10 秒 2 次
                perUser("order:driverFinish", 2, 10)    // 10 秒 2 次
        ));

        log.info("Sentinel 热点规则已加载: {}", ParamFlowRuleManager.getRules().size());
    }

    private ParamFlowRule perUser(String resource, int count, int durationSec) {
        ParamFlowRule rule = new ParamFlowRule(resource)
                .setParamIdx(0)              // 第 0 个参数 = userId
                .setCount(count);
        rule.setDurationInSec(durationSec);
        return rule;
    }
}