package com.example.javarelay.orderstatus.service;

import com.example.javarelay.orderstatus.dto.PassengerDto;
import com.example.javarelay.dto.Result;

/**
 * 乘客侧服务：只放"乘客自己发起"的动作，以及与之配套的重派 / 重算。
 *
 * <p>派单流程内部的步骤（乘客校验、路线、选司机、落库）在 {@link DispatchService}。
 */
public interface PassengerOperateService {

    /** 下单 */
    public Result PutOrder(PassengerDto passengerDto);

    /** 重派：重新入队，超过 60 秒取消 */
    public Result ReDispatchOrder(Long orderId, Long passengerId);

    /** 重新计算金额（订单结束后） */
    public Result ReComputeMoney(Long orderId);

    /** 取消订单 */
    public Result CancelOrder(Long passengerId);

    /** 完成订单 */
    public Result FinishOrder(Long passengerId);
}