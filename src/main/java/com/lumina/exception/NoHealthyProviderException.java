package com.lumina.exception;

/**
 * 轮询候选中存在未尝试的 Provider，但全部未通过健康检查（熔断器拒绝）。
 * 与"所有 Provider 已尝试过"区分开，调用方可据此触发保底降级而非终止 failover。
 */
public class NoHealthyProviderException extends RuntimeException {

    public NoHealthyProviderException(String message) {
        super(message);
    }
}
