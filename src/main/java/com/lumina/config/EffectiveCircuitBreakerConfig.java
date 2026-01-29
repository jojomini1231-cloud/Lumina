package com.lumina.config;

import lombok.Builder;
import lombok.Data;

/**
 * 生效的熔断器配置
 * 由 CircuitBreakerConfigResolver 解析后生成，所有字段都有值
 */
@Data
@Builder
public class EffectiveCircuitBreakerConfig {

    // ========== 触发条件 ==========
    private int minCalls;
    private double errorRateThreshold;
    private int consecutiveFailureThreshold;

    // ========== 慢调用配置 ==========
    private long slowCallThresholdMs;
    private double slowRateThreshold;

    // ========== 探测配置 ==========
    private int permittedCallsInHalfOpen;
    private int halfOpenSuccessThreshold;
    private int halfOpenFailureThreshold;
    private long halfOpenMaxDurationMs;

    // ========== 退避配置 ==========
    private long openBaseMs;
    private long openMaxMs;
    private double backoffMultiplier;
    private double jitterRatio;

    // ========== Failover 配置 ==========
    private int maxFailoverAttempts;

    // ========== Bulkhead 配置 ==========
    private int maxConcurrentRequestsPerProvider;

    // ========== 来源信息（用于调试） ==========
    private String sourceLevel;  // "global", "group", "provider"
    private String groupId;
    private String providerId;

    /**
     * 从全局配置创建
     */
    public static EffectiveCircuitBreakerConfig fromGlobal(CircuitBreakerConfig global) {
        return EffectiveCircuitBreakerConfig.builder()
                .minCalls(global.getMinCalls())
                .errorRateThreshold(global.getErrorRateThreshold())
                .consecutiveFailureThreshold(global.getConsecutiveFailureThreshold())
                .slowCallThresholdMs(global.getSlowCallThresholdMs())
                .slowRateThreshold(global.getSlowRateThreshold())
                .permittedCallsInHalfOpen(global.getPermittedCallsInHalfOpen())
                .halfOpenSuccessThreshold(global.getHalfOpenSuccessThreshold())
                .halfOpenFailureThreshold(global.getHalfOpenFailureThreshold())
                .halfOpenMaxDurationMs(global.getHalfOpenMaxDurationMs())
                .openBaseMs(global.getOpenBaseMs())
                .openMaxMs(global.getOpenMaxMs())
                .backoffMultiplier(global.getBackoffMultiplier())
                .jitterRatio(global.getJitterRatio())
                .maxFailoverAttempts(global.getMaxFailoverAttempts())
                .maxConcurrentRequestsPerProvider(global.getMaxConcurrentRequestsPerProvider())
                .sourceLevel("global")
                .build();
    }
}
