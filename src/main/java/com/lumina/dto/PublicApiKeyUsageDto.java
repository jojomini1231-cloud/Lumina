package com.lumina.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicApiKeyUsageDto {
    private String name;
    private Boolean enabled;
    private Boolean expired;
    private String status;
    private Long expiredAt;
    private Long maxRequests;
    private Long usedRequests;
    private Long availableRequests;
    private Boolean unlimitedRequests;
    private Long totalRequests;
    private Long successRequests;
    private BigDecimal totalCost;
    private String modelScope;
}
