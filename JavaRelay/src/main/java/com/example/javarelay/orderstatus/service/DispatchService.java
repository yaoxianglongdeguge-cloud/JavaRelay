package com.example.javarelay.orderstatus.service;

import com.example.javarelay.dto.Result;

/**
 * 派单流程服务：承载派单内部各步骤。
 *
 * <p>职责划分：
 * <ul>
 *   <li>{@link PassengerOperateService}：乘客侧动作（下单 / 重派 / 重算 / 取消 / 完成）</li>
 *   <li>{@code DispatchService}：派单内部步骤（乘客校验 / 选司机 / 路线 / 落库）</li>
 * </ul>
 *
 * <p>依赖方向是单向的：{@code PassengerOperateService -> DispatchService -> StrategysService}。
 * DispatchService 不再依赖 PassengerOperateService，
 * 而选车策略通过事件投递而不是直接注入 BatchDispatchTask，
 * 因此 {@code BatchDispatchTask -> DispatchService} 也不会成环。
 */
public interface DispatchService {

    /** 乘客校验（服务区等），通过后投递派单消息 */
    Result AssignPassenger(Long orderId, Long passengerId);

    /** 选司机：策略只负责入队，真正的分配由定时任务完成 */
    Result AssignDriver(Long orderId, Long passengerId);

    /** 路线 + 预估价 + 通知司机 */
    Result AssignPath(Long orderId, Long passengerId);

    /** 用指定司机完成派单：锁司机 -> 改订单状态 -> 进入路线环节 */
    Result applyAssign(Long orderId, Long passengerId, Long driverId);
}