package com.example.javarelay.pojo;

/**
 * 策略预设：三组权重。
 *
 * <p>替代了原来的 {@code AbstractBatchDispatchStrategy} + 三个子类，
 * 加新策略只需要在这里加一行。
 *
 * <p>注意：{@link #metric()} 返回的是共享实例，调用方只读，不要修改。
 */
public enum DispatchMetricPreset {

    /** 只挑最近的司机 */
    DEFAULT_1("default:1", 1.0D, 0.0D),

    /** 距离优先，评分轻微加权（1 分 ≈ 100 米） */
    DEFAULT_2("default:2", 1.0D, 0.5D),

    /** 更看重评分（1 分 ≈ 400 米） */
    DEFAULT_3("default:3", 1.0D, 2.0D);

    private final DispatchMetric metric;

    DispatchMetricPreset(String name, double distanceWeight, double scoreWeight) {
        this.metric = DispatchMetric.of(name, distanceWeight, scoreWeight);
    }

    public DispatchMetric metric() {
        return metric;
    }

    /** 按 type 查预设；未知类型回退到 DEFAULT_1 */
    public static DispatchMetricPreset of(String type) {
        if (type != null) {
            for (DispatchMetricPreset preset : values()) {
                if (preset.metric.getName().equals(type)) {
                    return preset;
                }
            }
        }
        return DEFAULT_1;
    }
}