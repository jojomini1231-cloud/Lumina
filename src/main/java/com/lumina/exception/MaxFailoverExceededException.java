package com.lumina.exception;

/**
 * Failover 次数超限异常
 * 当请求已尝试所有可用 Provider 或达到最大 Failover 次数时抛出
 */
public class MaxFailoverExceededException extends RuntimeException {

    private final int attemptCount;
    private final int maxAttempts;

    public MaxFailoverExceededException(int attemptCount, int maxAttempts) {
        super(String.format("Failover 次数已达上限: 已尝试 %d 次, 最大允许 %d 次", attemptCount, maxAttempts));
        this.attemptCount = attemptCount;
        this.maxAttempts = maxAttempts;
    }

    public MaxFailoverExceededException(String message, int attemptCount, int maxAttempts) {
        super(message);
        this.attemptCount = attemptCount;
        this.maxAttempts = maxAttempts;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }
}
