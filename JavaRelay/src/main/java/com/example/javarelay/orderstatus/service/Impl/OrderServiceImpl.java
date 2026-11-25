package com.example.javarelay.orderstatus.service.Impl;

import com.alibaba.fastjson2.JSON;
import com.example.javarelay.orderstatus.config.StrategyTypeConfig;
import com.example.javarelay.orderstatus.constants.Constants;
import com.example.javarelay.orderstatus.dto.PassengerDto;
import com.example.javarelay.dto.Result;
import com.example.javarelay.data.entity.Order;
import com.example.javarelay.data.entity.OrderDetail;
import com.example.javarelay.data.entity.OrderStatusLog;
import com.example.javarelay.orderstatus.pojo.RouteInfo;
import com.example.javarelay.orderstatus.exception.StatusChangeException;
import com.example.javarelay.data.mapper.DriverMapper;
import com.example.javarelay.data.mapper.OrderDetailMapper;
import com.example.javarelay.data.mapper.OrderMapper;
import com.example.javarelay.data.mapper.OrderStatusLogMapper;
import com.example.javarelay.orderstatus.service.OrderService;
import com.example.javarelay.orderstatus.constants.statusenum.DriverStatusEnum;
import com.example.javarelay.orderstatus.constants.statusenum.OrderOperatorEnum;
import com.example.javarelay.orderstatus.constants.statusenum.OrderStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    OrderMapper orderMapper;

    @Autowired
    DriverMapper driverMapper;

    @Autowired
    OrderStatusLogMapper orderStatusLogMapper;

    @Autowired
    OrderDetailMapper orderDetailMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result createAndInsertOrder(PassengerDto passengerDto){

        //生成订单并插入
        Long passengerId = passengerDto.getPassengerId();

        Order order1 = new Order(passengerId,
                OrderStatusEnum.WAITING_DISPATCH.getCode());

        try{
            orderMapper.insert(order1);

        }catch(DuplicateKeyException e) {
            //用唯一索引防止重复插入
            throw new StatusChangeException("ORDER_INSERT_FAIL");

        }
        //主键回填，得到订单id

        //生成订单状态流转日志并插入
        OrderStatusLog orderStatusLog = new OrderStatusLog(
                order1.getId(),
                OrderStatusEnum.WAITING_BIRTH.getCode(),
                OrderStatusEnum.WAITING_DISPATCH.getCode(),
                OrderOperatorEnum.PASSENGER.getCode(),
                passengerId
        );

        int rows = orderStatusLogMapper.insert(orderStatusLog);

        if (rows <= 0) {
            throw new RuntimeException("订单流转记录插入失败");
        }

        //生成并插入订单详情表
        OrderDetail orderDetail= new OrderDetail();
        orderDetail.setOrderId(order1.getId());
        orderDetail.setPassengerId(passengerId);
        orderDetail.setAcpriceType(passengerDto.getComputeMoneyType());
        orderDetail.setEspriceType(StrategyTypeConfig.PRECOMPUTE_MONEY_TYPE);
        orderDetail.setDriverType(passengerDto.getSelectDriverStrategy());
        orderDetail.setPassengerType(StrategyTypeConfig.PASSENGER_SELECT_TYPE);
        orderDetail.setPathType(StrategyTypeConfig.PATH_SELECT_TYPE);

        RouteInfo route = new RouteInfo();
        route.setStartAndEnd(
                passengerDto.getStartLng(), passengerDto.getStartLat(),
                passengerDto.getEndLng(), passengerDto.getEndLat());
        route.setStartAddress(passengerDto.getStartAddress());
        route.setEndAddress(passengerDto.getEndAddress());

        orderDetail.serializeRouteInfo(route);

        try{
            orderDetailMapper.insert(orderDetail);

        }catch(DuplicateKeyException e) {
            throw new StatusChangeException("订单详情表插入失败");

        }


        return Result.success(order1);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result changeOrderStatus(Long orderId,Long passengerId,
                                    Integer fromStatus,Integer toStatus,Integer operator,
                                    Long driverId,Integer fromStatus2,Integer toStatus2){


        int rows1 = 0;
        if(toStatus.equals(OrderStatusEnum.CANCELED.getCode())){
            rows1 = orderMapper.updateCancelWithOptimisticLock(orderId,
                    OrderStatusEnum.getCancelFromStatus()
                    ,toStatus,driverId);
        }else{
            rows1 = orderMapper.updateStatusWithOptimisticLock(orderId,fromStatus,toStatus,driverId);
        }
        if(rows1 == 0){
            throw new StatusChangeException("ORDER_CHANGE_FAIL");
        }else if(rows1 < 0){
            throw new RuntimeException("数据库异常");
        }

        // 重派补偿：从"已派单"退回"待派单"时，清空司机 / 路线 / 预估价。
        // 否则新司机派下来时，order_detail 还是上一个司机的数据（数据错位）。
        if (fromStatus.equals(OrderStatusEnum.DISPATCHED.getCode())
                && toStatus.equals(OrderStatusEnum.WAITING_DISPATCH.getCode())) {
            resetDispatchSnapshot(orderId);
        }

        if(toStatus.equals(OrderStatusEnum.CANCELED.getCode()) ||
                toStatus.equals(OrderStatusEnum.FINISHED.getCode())){

            int rows3 = orderMapper.updateActiveFlag(orderId, Constants.ACTIVE_ORDER,Constants.FINISHED_ORDER);
            if(rows3 == 0){
                throw new StatusChangeException("ORDER_CHANGE_FAIL");
            }else if(rows3 < 0){
                throw new RuntimeException("数据库异常");
            }

        }

        OrderStatusLog orderStatusLog = new OrderStatusLog(
                orderId,
                fromStatus,
                toStatus,
                operator,
                passengerId
        );

        int rows = orderStatusLogMapper.insert(orderStatusLog);
        if (rows == 0) {
            throw new RuntimeException("订单流转记录插入失败");
        }else if(rows < 0){
            throw new RuntimeException("数据库异常");
        }

        int rows2 = driverMapper.updateStatusWithOptimisticLock(driverId,fromStatus2,toStatus2);
        if(driverId != null && rows2 == 0){
            throw new StatusChangeException("DRIVER_CHANGE_FAIL");
        }else if(driverId != null && rows2 < 0){
            throw new RuntimeException("数据库异常");
        }


        return Result.success();
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelAfterDispatchFail(Long orderId, Long passengerId,
                                        Integer fromStatus, Long driverId, String reason) {

        int rows = orderMapper.updateStatusWithOptimisticLock(
                orderId, fromStatus, OrderStatusEnum.CANCELED.getCode(), null);
        if (rows == 0) {
            log.warn("补偿跳过：订单 {} 当前状态不是 {}，可能已被处理", orderId, fromStatus);
            return;
        }

        orderMapper.updateActiveFlag(orderId, Constants.ACTIVE_ORDER, Constants.FINISHED_ORDER);


        orderStatusLogMapper.insert(new OrderStatusLog(
                orderId, fromStatus, OrderStatusEnum.CANCELED.getCode(),
                OrderOperatorEnum.SYSTEM.getCode(), passengerId));

        if (driverId != null) {
            int dr = driverMapper.updateStatusWithOptimisticLock(
                    driverId,
                    DriverStatusEnum.RECEIVING.getCode(),
                    DriverStatusEnum.LEISURE.getCode());
            if (dr == 0) {
                log.warn("补偿：司机 {} 当前不是接单中，跳过", driverId);
            }
        }

        log.info("订单 {} 派单失败已补偿为取消，原因：{}", orderId, reason);
    }

    /**
     * 重派补偿：把订单详情退回"只有起终点"，清空司机 / 路线 / 预估价。
     */
    private void resetDispatchSnapshot(Long orderId) {

        OrderDetail detail = orderDetailMapper.selectByOrderId(orderId);

        if (detail == null) {
            log.warn("重派补偿跳过：订单详情不存在 orderId={}", orderId);
            return;
        }

        // 已结算的订单不动
        if (detail.getActualPrice() != null) {
            log.info("重派补偿跳过：订单已结算 orderId={}", orderId);
            return;
        }

        String minimal = null;
        RouteInfo base = detail.deserializeRouteInfo();

        if (base != null) {
            RouteInfo keep = new RouteInfo();
            keep.setStartAndEnd(base.getStartLng(), base.getStartLat(), base.getEndLng(), base.getEndLat());
            keep.setStartAddress(base.getStartAddress());
            keep.setEndAddress(base.getEndAddress());
            minimal = JSON.toJSONString(keep);
        }

        int rows = orderDetailMapper.resetForRedispatch(orderId, minimal);

        log.info("重派补偿：清空司机/路线/预估价 orderId={} rows={}", orderId, rows);
    }
}
