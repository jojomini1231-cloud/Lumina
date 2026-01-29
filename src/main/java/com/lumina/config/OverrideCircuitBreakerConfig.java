package com.lumina.config;

import lombok.Data;

/**
 * 可覆盖的熔断器配置
 * 用于 Group 或 Provider 级别覆盖全局配置
 * 所有字段为 null 表示使用上级配置（全局或 Group 级别）
 */
@Data
public class OverrideCircuitBreakerConfig {

    // ========== 触发条件 ==========

    /**
     * 最小请求数
     */
    private Integer minCalls;

    /**
     * 错误率阈值
     */
    private Double errorRateThreshold;

    /**
     * 连续失败阈值
     */
    private Integer consecutiveFailureThreshold;

    // ========== 慢调用配置 ==========

    /**
     * 慢调用阈值（毫秒）
     */
    private Long slowCallThresholdMs;

    /**
     * 慢调用率阈值
     */
    private Double slowRateThreshold;

    // ========== 探测配置 ==========

    /**
     * HALF_OPEN 允许的探测请求数
     */
    private Integer permittedCallsInHalfOpen;

    /**
     * HALF_OPEN 成功阈值
     */
    private Integer halfOpenSuccessThreshold;

    /**
     * HALF_OPEN 失败阈值
     */
    private Integer halfOpenFailureThreshold;

    /**
     * HALF_OPEN 最大持续时间
     */
    private Long halfOpenMaxDurationMs;

    // ========== 退避配置 ==========

    /**
     * OPEN 基础时间
     */
    private Long openBaseMs;

    /**
     * OPEN 最大时间
     */
    private Long openMaxMs;

    /**
     * 退避乘数
     */
    private Double backoffMultiplier;

    /**
     * 抖动比例
     */
    private Double jitterRatio;

    // ========== Failover 配置 ==========

    /**
     * 最大 Failover 次数
     */
    private Integer maxFailoverAttempts;

    // ========== Bulkhead 配置 ==========

    /**
     * 每 Provider 最大并发数
     */
    private Integer maxConcurrentRequestsPerProvider;

    // ========== 灰度配置 ==========

    /**
     * 灰度流量百分比 (0-100)
     * 表示多少比例的请求使用此覆盖配置
     * null 或 100 表示全量生效
     */
    private Integer grayscalePercent;

    /**
     * 是否启用此覆盖配置
     */
    private Boolean enabled;

    /**
     * 检查配置是否启用
     */
    public boolean isEffectivelyEnabled() {
        return enabled == null || enabled;
    }

    /**
     * 检查是否应该对当前请求应用灰度
     * @param requestHash 请求哈希值（用于一致性）
     * @return 是否应用此配置
     */
    public boolean shouldApplyGrayscale(int requestHash) {
        if (grayscalePercent == null || grayscalePercent >= 100) {
            return true;
        }
        if (grayscalePercent <= 0) {
            return false;
        }
        // 使用 hash 保证同一请求/用户的一致性
        return Math.abs(requestHash % 100) < grayscalePercent;
    }
}
