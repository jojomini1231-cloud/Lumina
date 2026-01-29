package com.lumina.state;

/**
 * 错误类型枚举，控制熔断和 Failover 行为
 */
public enum FailureType {

    SUCCESS(false, false),

    // 强信号，触发熔断 + Failover
    TIMEOUT(true, true),
    CONNECT(true, true),
    DNS(true, true),
    TLS(true, true),
    HTTP_5XX(true, true),

    // 限流，可 Failover
    HTTP_429(true, true),

    // 客户端错误，不计入熔断，不 Failover
    HTTP_4XX(false, false),

    // 解码错误，计入熔断但不 Failover（可能是请求格式问题）
    DECODE(true, false),

    // 未知错误，默认触发熔断和 Failover
    UNKNOWN(true, true);

    private final boolean countsAsFailure;
    private final boolean shouldFailover;

    FailureType(boolean countsAsFailure, boolean shouldFailover) {
        this.countsAsFailure = countsAsFailure;
        this.shouldFailover = shouldFailover;
    }

    /**
     * 是否计入熔断统计
     */
    public boolean countsAsFailure() {
        return countsAsFailure;
    }

    /**
     * 是否应切换 Provider
     */
    public boolean shouldFailover() {
        return shouldFailover;
    }
}
