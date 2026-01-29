package com.lumina.dto;

import com.lumina.config.OverrideCircuitBreakerConfig;
import lombok.Data;

import java.util.List;

@Data
public class ModelGroupConfig {

    private String id;

    private String name;

    private Integer balanceMode;

    private Integer firstTokenTimeout;

    private List<ModelGroupConfigItem> items;

    /**
     * Group 级别熔断器配置覆盖
     * 可覆盖全局配置的部分或全部参数
     */
    private OverrideCircuitBreakerConfig circuitBreakerConfig;
}
