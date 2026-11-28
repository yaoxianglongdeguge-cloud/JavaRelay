package com.example.javarelay.orderstatus.dispatch.mqtask.messagecells;

import com.example.javarelay.infra.mq.MessageCell;
import com.example.javarelay.orderstatus.config.MQConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


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