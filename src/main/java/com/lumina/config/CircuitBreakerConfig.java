package com.lumina.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 熔断器配置类，外部化所有阈值参数
 */
@Data
@Component
@ConfigurationProperties(prefix = "lumina.circuit-breaker")
public class CircuitBreakerConfig {

    // ========== 触发条件 ==========

    /**
     * 最小请求数，达到此数量后才计算错误率
     */
    private int minCalls = 20;

    /**
     * 错误率阈值（0.0 - 1.0），超过此阈值触发熔断
     */
    private double errorRateThreshold = 0.5;

    /**
     * 连续失败阈值，连续失败达到此数量时强制熔断
     */
    private int consecutiveFailureThreshold = 5;

    // ========== 慢调用配置 ==========

    /**
     * 慢调用阈值（毫秒），响应时间超过此值视为慢调用
     */
    private long slowCallThresholdMs = 4000;

    /**
     * 慢调用率阈值（0.0 - 1.0），超过此阈值触发熔断
     */
    private double slowRateThreshold = 0.6;

    // ========== 探测配置（HALF_OPEN 状态） ==========

    /**
     * HALF_OPEN 状态允许的探测请求数
     */
    private int permittedCallsInHalfOpen = 2;

    /**
     * HALF_OPEN 状态下成功数达到此阈值时关闭熔断
     */
    private int halfOpenSuccessThreshold = 2;

    /**
     * HALF_OPEN 状态下失败数达到此阈值时重新打开熔断
     */
    private int halfOpenFailureThreshold = 1;

    /**
     * HALF_OPEN 状态最大持续时间（毫秒），超时未决策则转回 OPEN
     */
    private long halfOpenMaxDurationMs = 30000;

    // ========== 退避配置 ==========

    /**
     * OPEN 状态基础持续时间（毫秒）
     */
    private long openBaseMs = 5000;

    /**
     * OPEN 状态最大持续时间（毫秒）
     */
    private long openMaxMs = 300000;

    /**
     * 退避乘数
     */
    private double backoffMultiplier = 2.0;

    /**
     * 抖动比例（0.0 - 1.0）
     */
    private double jitterRatio = 0.2;

    // ========== Failover 配置 ==========

    /**
     * 最大 Failover 尝试次数
     */
    private int maxFailoverAttempts = 3;

    // ========== Bulkhead（并发舱壁）配置 ==========

    /**
     * 每个 Provider 最大并发请求数
     */
    private int maxConcurrentRequestsPerProvider = 50;

    // ========== 滑动窗口配置 ==========

    /**
     * 滑动窗口桶数量
     */
    private int windowBucketCount = 10;

    /**
     * 每个桶的时间跨度（毫秒）
     */
    private long windowBucketDurationMs = 1000;
}
