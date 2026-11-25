package com.example.javarelay.orderstatus.mqtask.messagecells;

import com.example.javarelay.infra.mq.MessageCell;
import com.example.javarelay.orderstatus.config.MQConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 派单链路消息：一个 topic、一个 step 字段，表示"推进到哪一步"。
 *
 * <p>设计约定：
 * <ul>
 *   <li>消息只带 orderId / passengerId / step，<b>不带业务数据</b>；
 *       每一步都重新读订单，所以天然幂等、天然自愈</li>
 *   <li>步骤顺序：PASSENGER → DRIVER → PATH，由各步骤成功后自行发下一条</li>
 *   <li>失败重试不走这里（见 ReDispatchMessage，它带 retryCount）</li>
 * </ul>
 */
public class DispatchMessage extends MessageCell<DispatchMessage.msg> {

    /** 派单链路的步骤 */
    public enum Step {

        /** 乘客校验（服务区等） */
        PASSENGER,

        /** 选司机（策略只负责入队） */
        DRIVER,

        /** 路线 + 预估价 + 通知司机 */
        PATH
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class msg {

        Long orderId;

        Long passengerId;

        /** 推进到哪一步（必填） */
        Step step;
    }

    public DispatchMessage() {
    }

    public DispatchMessage(Long orderId, Long passengerId, Step step) {
        this.setTopic(MQConfig.MESSAGE_DISPATCH_TOPIC);
        this.setMsg(new msg(orderId, passengerId, step));
    }
}