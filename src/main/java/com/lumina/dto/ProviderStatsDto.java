package com.lumina.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 供应商统计排名数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderStatsDto {
    /**
     * 排名
     */
    private Integer rank;

    /**
     * 供应商ID
     */
    private Long providerId;

    /**
     * 供应商名称
     */
    private String providerName;

    /**
     * 调用次数
     */
    private Long callCount;

    /**
     * 预估费用
     */
    private BigDecimal estimatedCost;

    /**
     * 平均延迟（毫秒）
     */
    private Double avgLatency;

    /**
     * 成功率（百分比）
     */
    private Double successRate;

}
