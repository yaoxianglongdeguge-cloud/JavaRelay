package com.example.javarelay.orderstatus.dispatch;

import lombok.Getter;

import java.util.List;

/**
 * 一轮匹配的收益矩阵：行 = 订单，列 = 候选司机。
 */
@Getter
public class DispatchMatrix {

    /** 列：候选司机 id（顺序与矩阵列一一对应） */
    private final List<Long> driverIds;

    /** 收益：越大越优先 */
    private final long[][] profit;

    /** 是否允许派：半径内 + 状态可用 */
    private final boolean[][] allowed;

    /** GEO 半径内的司机总数（去重前，用于观测） */
    private final int candidateCount;

    /** 因超出某个订单半径而不可派的组合数 */
    private final int noDistanceCount;

    /** 属性缓存缺失的司机数（去重后） */
    private final int attrMissingCount;

    public DispatchMatrix(List<Long> driverIds, long[][] profit, boolean[][] allowed,
                          int candidateCount, int noDistanceCount, int attrMissingCount) {
        this.driverIds = driverIds;
        this.profit = profit;
        this.allowed = allowed;
        this.candidateCount = candidateCount;
        this.noDistanceCount = noDistanceCount;
        this.attrMissingCount = attrMissingCount;
    }

    public int rows() {
        return profit.length;
    }

    public int cols() {
        return driverIds.size();
    }

    public boolean isEmpty() {
        return driverIds.isEmpty();
    }
}