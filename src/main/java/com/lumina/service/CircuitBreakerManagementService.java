package com.lumina.service;

import com.lumina.config.CircuitBreakerConfig;
import com.lumina.dto.CircuitBreakerControlRequest;
import com.lumina.dto.CircuitBreakerStatusResponse;
import com.lumina.state.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 熔断器管控服务
 * 提供手动控制熔断器状态的能力
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CircuitBreakerManagementService {

    private final ProviderStateRegistry providerStateRegistry;
    private final CircuitBreakerConfig circuitBreakerConfig;
    private final CircuitBreakerEventLogger eventLogger;

    /**
     * 获取单个 Provider 的熔断器状态
     */
    public CircuitBreakerStatusResponse getStatus(String providerId) {
        ProviderRuntimeState state = providerStateRegistry.getIfExists(providerId);
        if (state == null) {
            return null;
        }
        return buildStatusResponse(state);
    }

    /**
     * 获取所有 Provider 的熔断器状态列表
     */
    public List<CircuitBreakerStatusResponse> listAllStatus() {
        return providerStateRegistry.getAllProviders()
                .stream()
                .map(this::buildStatusResponse)
                .collect(Collectors.toList());
    }

    /**
     * 手动控制熔断器状态
     */
    public CircuitBreakerStatusResponse controlCircuitBreaker(CircuitBreakerControlRequest request, String operator) {
        String providerId = request.getProviderId();
        ProviderRuntimeState state = providerStateRegistry.get(providerId);

        CircuitState previousState = state.getCircuitState();
        CircuitState targetState = mapTargetState(request.getTargetState());

        String reason = request.getReason() != null ? request.getReason() : "Manual control";

        switch (request.getTargetState()) {
            case OPEN:
                forceOpen(state, request.getDurationMs(), reason, operator);
                break;
            case CLOSED:
                forceClose(state, reason, operator);
                break;
            case HALF_OPEN:
                forceHalfOpen(state, reason, operator);
                break;
        }

        log.info("手动控制熔断器: providerId={}, {} -> {}, operator={}, reason={}",
                providerId, previousState, targetState, operator, reason);

        eventLogger.logManualControl(state, previousState, targetState, reason, operator);

        return buildStatusResponse(state);
    }

    /**
     * 释放手动控制，恢复自动管理
     */
    public CircuitBreakerStatusResponse releaseManualControl(String providerId) {
        ProviderRuntimeState state = providerStateRegistry.getIfExists(providerId);
        if (state == null) {
            return null;
        }

        state.disableManualControl();
        log.info("释放手动控制: providerId={}, 恢复自动管理", providerId);

        return buildStatusResponse(state);
    }

    /**
     * 强制打开熔断器
     */
    private void forceOpen(ProviderRuntimeState state, Long durationMs, String reason, String operator) {
        state.enableManualControl(reason, operator);

        // 计算 OPEN 持续时间
        long openDuration = durationMs != null ? durationMs : circuitBreakerConfig.getOpenBaseMs();
        long nextProbeAt = System.currentTimeMillis() + openDuration;

        state.forceTransitionTo(CircuitState.OPEN);
        state.setCircuitOpenedAt(System.currentTimeMillis());
        state.setNextProbeAt(nextProbeAt);

        log.info("强制打开熔断: providerId={}, duration={}ms, nextProbeAt={}",
                state.getProviderId(), openDuration, nextProbeAt);
    }

    /**
     * 强制关闭熔断器
     */
    private void forceClose(ProviderRuntimeState state, String reason, String operator) {
        state.enableManualControl(reason, operator);

        state.forceTransitionTo(CircuitState.CLOSED);
        state.resetOnClose();

        log.info("强制关闭熔断: providerId={}", state.getProviderId());
    }

    /**
     * 强制进入 HALF_OPEN 状态
     */
    private void forceHalfOpen(ProviderRuntimeState state, String reason, String operator) {
        state.enableManualControl(reason, operator);

        state.forceTransitionTo(CircuitState.HALF_OPEN);
        state.initHalfOpen(circuitBreakerConfig.getPermittedCallsInHalfOpen());

        log.info("强制进入 HALF_OPEN: providerId={}, permittedCalls={}",
                state.getProviderId(), circuitBreakerConfig.getPermittedCallsInHalfOpen());
    }

    /**
     * 转换目标状态枚举
     */
    private CircuitState mapTargetState(CircuitBreakerControlRequest.TargetState targetState) {
        switch (targetState) {
            case OPEN:
                return CircuitState.OPEN;
            case CLOSED:
                return CircuitState.CLOSED;
            case HALF_OPEN:
                return CircuitState.HALF_OPEN;
            default:
                throw new IllegalArgumentException("Unknown target state: " + targetState);
        }
    }

    /**
     * 构建状态响应对象
     */
    private CircuitBreakerStatusResponse buildStatusResponse(ProviderRuntimeState state) {
        ProviderBulkhead bulkhead = state.getBulkhead();

        return CircuitBreakerStatusResponse.builder()
                .providerId(state.getProviderId())
                .providerName(state.getProviderName())
                .circuitState(state.getCircuitState())
                .circuitOpenedAt(state.getCircuitOpenedAt())
                .nextProbeAt(state.getNextProbeAt())
                .openAttempt(state.getOpenAttempt())
                .score(state.getScore())
                .errorRate(state.getWindowErrorRate())
                .slowRate(state.getWindowSlowRate())
                .consecutiveFailures(state.getConsecutiveFailures().get())
                .totalRequests(state.getTotalRequests().get())
                .successRequests(state.getSuccessRequests().get())
                .failureRequests(state.getFailureRequests().get())
                .currentConcurrent(bulkhead.getCurrentConcurrent())
                .maxConcurrent(bulkhead.getMaxConcurrent())
                .bulkheadRejectedCount(bulkhead.getRejectedCount())
                .probeRemaining(state.getProbeRemaining().get())
                .halfOpenSuccessCount(state.getHalfOpenSuccessCount().get())
                .halfOpenFailureCount(state.getHalfOpenFailureCount().get())
                .manuallyControlled(state.isManuallyControlled())
                .manualControlReason(state.getManualControlReason())
                .build();
    }
}
