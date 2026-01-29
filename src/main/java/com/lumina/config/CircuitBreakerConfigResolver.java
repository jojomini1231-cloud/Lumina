package com.lumina.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 熔断器配置解析器
 * 按优先级合并配置：Provider > Group > Global
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CircuitBreakerConfigResolver {

    private final CircuitBreakerConfig globalConfig;

    /**
     * 解析生效配置（仅全局）
     */
    public EffectiveCircuitBreakerConfig resolve() {
        return EffectiveCircuitBreakerConfig.fromGlobal(globalConfig);
    }

    /**
     * 解析生效配置（Group 级别覆盖）
     * @param groupId Group ID
     * @param groupOverride Group 级别覆盖配置（可为 null）
     */
    public EffectiveCircuitBreakerConfig resolve(String groupId, OverrideCircuitBreakerConfig groupOverride) {
        return resolve(groupId, groupOverride, null, null, 0);
    }

    /**
     * 解析生效配置（完整版，支持 Group + Provider 覆盖 + 灰度）
     * @param groupId Group ID
     * @param groupOverride Group 级别覆盖配置（可为 null）
     * @param providerId Provider ID
     * @param providerOverride Provider 级别覆盖配置（可为 null）
     * @param requestHash 请求哈希（用于灰度一致性）
     */
    public EffectiveCircuitBreakerConfig resolve(
            String groupId,
            OverrideCircuitBreakerConfig groupOverride,
            String providerId,
            OverrideCircuitBreakerConfig providerOverride,
            int requestHash
    ) {
        // 从全局配置开始
        EffectiveCircuitBreakerConfig.EffectiveCircuitBreakerConfigBuilder builder =
                EffectiveCircuitBreakerConfig.builder()
                        .minCalls(globalConfig.getMinCalls())
                        .errorRateThreshold(globalConfig.getErrorRateThreshold())
                        .consecutiveFailureThreshold(globalConfig.getConsecutiveFailureThreshold())
                        .slowCallThresholdMs(globalConfig.getSlowCallThresholdMs())
                        .slowRateThreshold(globalConfig.getSlowRateThreshold())
                        .permittedCallsInHalfOpen(globalConfig.getPermittedCallsInHalfOpen())
                        .halfOpenSuccessThreshold(globalConfig.getHalfOpenSuccessThreshold())
                        .halfOpenFailureThreshold(globalConfig.getHalfOpenFailureThreshold())
                        .halfOpenMaxDurationMs(globalConfig.getHalfOpenMaxDurationMs())
                        .openBaseMs(globalConfig.getOpenBaseMs())
                        .openMaxMs(globalConfig.getOpenMaxMs())
                        .backoffMultiplier(globalConfig.getBackoffMultiplier())
                        .jitterRatio(globalConfig.getJitterRatio())
                        .maxFailoverAttempts(globalConfig.getMaxFailoverAttempts())
                        .maxConcurrentRequestsPerProvider(globalConfig.getMaxConcurrentRequestsPerProvider())
                        .sourceLevel("global")
                        .groupId(groupId)
                        .providerId(providerId);

        String sourceLevel = "global";

        // 应用 Group 级别覆盖
        if (groupOverride != null && groupOverride.isEffectivelyEnabled()
                && groupOverride.shouldApplyGrayscale(requestHash)) {
            applyOverride(builder, groupOverride);
            sourceLevel = "group";
            log.debug("应用 Group 级别配置覆盖: groupId={}", groupId);
        }

        // 应用 Provider 级别覆盖（优先级更高）
        if (providerOverride != null && providerOverride.isEffectivelyEnabled()
                && providerOverride.shouldApplyGrayscale(requestHash)) {
            applyOverride(builder, providerOverride);
            sourceLevel = "provider";
            log.debug("应用 Provider 级别配置覆盖: providerId={}", providerId);
        }

        return builder.sourceLevel(sourceLevel).build();
    }

    /**
     * 将覆盖配置应用到 builder（仅覆盖非 null 字段）
     */
    private void applyOverride(
            EffectiveCircuitBreakerConfig.EffectiveCircuitBreakerConfigBuilder builder,
            OverrideCircuitBreakerConfig override
    ) {
        if (override.getMinCalls() != null) {
            builder.minCalls(override.getMinCalls());
        }
        if (override.getErrorRateThreshold() != null) {
            builder.errorRateThreshold(override.getErrorRateThreshold());
        }
        if (override.getConsecutiveFailureThreshold() != null) {
            builder.consecutiveFailureThreshold(override.getConsecutiveFailureThreshold());
        }
        if (override.getSlowCallThresholdMs() != null) {
            builder.slowCallThresholdMs(override.getSlowCallThresholdMs());
        }
        if (override.getSlowRateThreshold() != null) {
            builder.slowRateThreshold(override.getSlowRateThreshold());
        }
        if (override.getPermittedCallsInHalfOpen() != null) {
            builder.permittedCallsInHalfOpen(override.getPermittedCallsInHalfOpen());
        }
        if (override.getHalfOpenSuccessThreshold() != null) {
            builder.halfOpenSuccessThreshold(override.getHalfOpenSuccessThreshold());
        }
        if (override.getHalfOpenFailureThreshold() != null) {
            builder.halfOpenFailureThreshold(override.getHalfOpenFailureThreshold());
        }
        if (override.getHalfOpenMaxDurationMs() != null) {
            builder.halfOpenMaxDurationMs(override.getHalfOpenMaxDurationMs());
        }
        if (override.getOpenBaseMs() != null) {
            builder.openBaseMs(override.getOpenBaseMs());
        }
        if (override.getOpenMaxMs() != null) {
            builder.openMaxMs(override.getOpenMaxMs());
        }
        if (override.getBackoffMultiplier() != null) {
            builder.backoffMultiplier(override.getBackoffMultiplier());
        }
        if (override.getJitterRatio() != null) {
            builder.jitterRatio(override.getJitterRatio());
        }
        if (override.getMaxFailoverAttempts() != null) {
            builder.maxFailoverAttempts(override.getMaxFailoverAttempts());
        }
        if (override.getMaxConcurrentRequestsPerProvider() != null) {
            builder.maxConcurrentRequestsPerProvider(override.getMaxConcurrentRequestsPerProvider());
        }
    }

    /**
     * 获取全局配置
     */
    public CircuitBreakerConfig getGlobalConfig() {
        return globalConfig;
    }
}
