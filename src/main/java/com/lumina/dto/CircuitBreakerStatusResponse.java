package com.lumina.dto;

import com.lumina.state.CircuitState;
import lombok.Builder;
import lombok.Data;

/**
 * Provider 熔断器状态信息
 */
@Data
@Builder
public class CircuitBreakerStatusResponse {

    private String providerId;
    private String providerName;

    // 熔断状态
    private CircuitState circuitState;
    private long circuitOpenedAt;
    private long nextProbeAt;
    private int openAttempt;

    // 统计信息
    private double score;
    private double errorRate;
    private double slowRate;
    private int consecutiveFailures;
    private long totalRequests;
    private long successRequests;
    private long failureRequests;

    // 并发舱壁
    private int currentConcurrent;
    private int maxConcurrent;
    private long bulkheadRejectedCount;

    // HALF_OPEN 探测信息
    private int probeRemaining;
    private int halfOpenSuccessCount;
    private int halfOpenFailureCount;

    // 是否为手动控制
    private boolean manuallyControlled;
    private String manualControlReason;
}
