package com.example.javarelay.globalconfig;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
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

        // 热点参数限流依赖额外的 SlotChain，必须在第一个请求之前切换
        System.setProperty("csp.sentinel.slotchain.builder",
                "com.alibaba.csp.sentinel.slots.HotParamSlotChainBuilder");

        // ① 按用户限流（热点参数，paramIdx=0 即 userId）
        ParamFlowRuleManager.loadRules(List.of(
                perUser("order:put",          1, 5),    // 5 秒 1 次
                perUser("order:cancel",       3, 10),   // 10 秒 3 次
                perUser("order:finish",       2, 10),   // 10 秒 2 次
                perUser("order:receive",      5, 1),    // 1 秒 5 次
                perUser("order:arrive",       2, 10),   // 10 秒 2 次
                perUser("order:driverFinish", 2, 10)    // 10 秒 2 次
        ));

        // ② 接口总量限流（防登录接口被刷）
        FlowRuleManager.loadRules(List.of(
                new FlowRule("login:signin")
                        .setGrade(RuleConstant.FLOW_GRADE_QPS)
                        .setCount(20)
        ));

        log.info("Sentinel 规则已加载: 热点={}, 普通={}",
                ParamFlowRuleManager.getRules().size(),
                FlowRuleManager.getRules().size());
    }

    private ParamFlowRule perUser(String resource, int count, int durationSec) {
        ParamFlowRule rule = new ParamFlowRule(resource)
                .setParamIdx(0)              // 第 0 个参数 = userId
                .setCount(count);
        rule.setDurationInSec(durationSec);
        return rule;
    }
}
