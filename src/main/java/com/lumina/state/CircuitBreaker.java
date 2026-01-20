package com.lumina.state;

import org.springframework.stereotype.Component;

@Component
public class CircuitBreaker {

    private static final int FAILURE_THRESHOLD = 5;
    private static final long OPEN_DURATION_MS = 30_000;

    /**
     * 判断是否允许请求通过
     * 使用 CAS 操作确保并发安全
     */
    public boolean allowRequest(ProviderRuntimeState stats) {
        CircuitState currentState = stats.getCircuitState();

        // CLOSED 状态：允许所有请求
        if (currentState == CircuitState.CLOSED) {
            return true;
        }

        // OPEN 状态：检查是否超过冷却时间
        if (currentState == CircuitState.OPEN) {
            long openedAt = stats.getCircuitOpenedAt();
            if (System.currentTimeMillis() - openedAt > OPEN_DURATION_MS) {
                // 使用 CAS 确保只有一个线程能将状态从 OPEN 转换到 HALF_OPEN
                if (stats.compareAndSetCircuitState(CircuitState.OPEN, CircuitState.HALF_OPEN)) {
                    // CAS 成功，允许这个线程进行试探请求
                    return true;
                }
                // CAS 失败，说明其他线程已经转换了状态，拒绝本次请求
                return false;
            }
            // 未超过冷却时间，拒绝请求
            return false;
        }

        // HALF_OPEN 状态：使用 CAS 确保只有一个请求能通过试探
        return stats.tryAcquireHalfOpenRequest();
    }

    /**
     * 请求成功时的处理
     * 重置熔断器状态和失败计数
     */
    public void onSuccess(ProviderRuntimeState stats) {
        stats.setCircuitState(CircuitState.CLOSED);
        stats.getFailureRequests().set(0);
    }

    /**
     * 请求失败时的处理
     * 使用 CAS 操作确保状态转换的原子性
     */
    public void onFailure(ProviderRuntimeState stats) {
        // 原子性地增加失败计数
        int failures = stats.getFailureRequests().incrementAndGet();

        // 检查是否需要打开熔断器
        // 条件：失败次数达到阈值 或 评分过低
        if (failures >= FAILURE_THRESHOLD || stats.getScore() < 40) {
            CircuitState currentState = stats.getCircuitState();

            // 只有在非 OPEN 状态时才尝试打开熔断器
            if (currentState != CircuitState.OPEN) {
                // 使用 CAS 确保状态转换的原子性，避免多个线程同时打开熔断器
                if (stats.compareAndSetCircuitState(currentState, CircuitState.OPEN)) {
                    // CAS 成功，设置熔断器打开时间
                    stats.setCircuitOpenedAt(System.currentTimeMillis());
                }
                // CAS 失败说明其他线程已经修改了状态，无需处理
            }
        }
    }
}
