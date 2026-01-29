package com.lumina.dto;

import com.lumina.config.OverrideCircuitBreakerConfig;
import lombok.Data;

import java.util.List;

@Data
public class ModelGroupConfigItem {

    private Long providerId;

    private String providerName;

    private String modelName;

    private Integer weight;

    private String apiKey;

    private String baseUrl;

    /**
     * Provider 级别熔断器配置覆盖
     * 优先级高于 Group 级别配置
     */
    private OverrideCircuitBreakerConfig circuitBreakerConfig;
}
