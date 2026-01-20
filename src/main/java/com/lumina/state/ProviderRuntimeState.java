package com.lumina.state;

import lombok.Data;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
public class ProviderRuntimeState {

    private final String providerId;
    private volatile String providerName;

    // 统计窗口
    private long windowStart = System.currentTimeMillis();

    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger successRequests = new AtomicInteger(0);
    private final AtomicInteger failureRequests = new AtomicInteger(0);

    // 延迟 EMA（指数滑动平均）
    private volatile double latencyEmaMs = 0;

    // 成功率 EMA
    private volatile double successRateEma = 1.0;

    // 当前评分（0 ~ 100）
    private volatile double score = 100;

    // 熔断状态 - 使用 AtomicReference 支持 CAS 操作
    private final AtomicReference<CircuitState> circuitState = new AtomicReference<>(CircuitState.CLOSED);
    private volatile long circuitOpenedAt = 0;

    // HALF_OPEN 状态下的请求控制标志，确保只有一个请求能通过
    private final AtomicBoolean halfOpenRequestInProgress = new AtomicBoolean(false);

    public ProviderRuntimeState(String providerId) {
        this.providerId = providerId;
    }

    // 获取熔断器状态
    public CircuitState getCircuitState() {
        return circuitState.get();
    }

    // 设置熔断器状态
    public void setCircuitState(CircuitState state) {
        circuitState.set(state);
        // 当状态变为 CLOSED 或 OPEN 时，重置 HALF_OPEN 请求标志
        if (state == CircuitState.CLOSED || state == CircuitState.OPEN) {
            halfOpenRequestInProgress.set(false);
        }
    }

    // CAS 操作：原子性地更新熔断器状态
    public boolean compareAndSetCircuitState(CircuitState expect, CircuitState update) {
        return circuitState.compareAndSet(expect, update);
    }

    // 检查在 HALF_OPEN 状态下是否允许请求通过
    // 使用 CAS 确保只有一个线程能成功获取试探机会
    public boolean tryAcquireHalfOpenRequest() {
        return circuitState.get() == CircuitState.HALF_OPEN
            && halfOpenRequestInProgress.compareAndSet(false, true);
    }

    // 为了兼容现有代码，保留一些存根方法或进行重构
    @Deprecated
    public boolean isAvailable() {
        return circuitState.get() != CircuitState.OPEN;
    }

    @Deprecated
    public int getCurrentWeight() {
        return (int) score;
    }
}
