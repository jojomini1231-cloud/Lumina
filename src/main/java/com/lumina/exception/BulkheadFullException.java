package com.lumina.exception;

/**
 * 并发舱壁满异常（阶段2预留）
 * 当 Provider 的并发请求数达到上限时抛出
 */
public class BulkheadFullException extends RuntimeException {

    private final String providerId;
    private final int currentConcurrency;
    private final int maxConcurrency;

    public BulkheadFullException(String providerId, int currentConcurrency, int maxConcurrency) {
        super(String.format("Provider %s 并发已满: 当前 %d, 上限 %d", providerId, currentConcurrency, maxConcurrency));
        this.providerId = providerId;
        this.currentConcurrency = currentConcurrency;
        this.maxConcurrency = maxConcurrency;
    }

    public String getProviderId() {
        return providerId;
    }

    public int getCurrentConcurrency() {
        return currentConcurrency;
    }

    public int getMaxConcurrency() {
        return maxConcurrency;
    }
}
