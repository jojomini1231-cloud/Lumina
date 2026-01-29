package com.lumina.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 熔断器手动控制请求
 */
@Data
public class CircuitBreakerControlRequest {

    /**
     * Provider ID
     */
    @NotBlank(message = "providerId 不能为空")
    private String providerId;

    /**
     * 目标状态：OPEN, CLOSED, HALF_OPEN
     */
    @NotNull(message = "targetState 不能为空")
    private TargetState targetState;

    /**
     * 操作原因（用于审计日志）
     */
    private String reason;

    /**
     * OPEN 状态持续时间（毫秒），仅当 targetState=OPEN 时有效
     * 不设置则使用默认退避时间
     */
    private Long durationMs;

    public enum TargetState {
        OPEN,       // 强制打开熔断
        CLOSED,     // 强制关闭熔断
        HALF_OPEN   // 强制进入半开状态
    }
}
