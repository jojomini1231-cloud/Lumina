package com.lumina.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("provider_runtime_stats")
public class ProviderRuntimeStats {

    @TableId
    private String providerId;
    private String providerName;

    private Double successRateEma;
    private Double latencyEmaMs;
    private Double score;

    private Integer totalRequests;
    private Integer successRequests;
    private Integer failureRequests;

    private String circuitState;
    private Long circuitOpenedAt;

    // 新增字段：熔断/容错机制优化
    private Integer consecutiveFailures;
    private Integer openAttempt;
    private Long nextProbeAt;

    private LocalDateTime updatedAt;
}
