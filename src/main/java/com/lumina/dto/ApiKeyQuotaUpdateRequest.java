package com.lumina.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ApiKeyQuotaUpdateRequest {
    private BigDecimal maxAmount;
    private Long maxRequests;
    private Long maxConcurrentRequests;
    private java.util.List<String> supportedModels;
    private String keyGroup;
}
