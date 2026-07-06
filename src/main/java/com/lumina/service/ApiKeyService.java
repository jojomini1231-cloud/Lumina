package com.lumina.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lumina.entity.ApiKey;
import reactor.core.publisher.Mono;

public interface ApiKeyService extends IService<ApiKey> {
    ApiKey generateApiKey(String name);

    ApiKey generateApiKey(String name, String keyGroup);

    Mono<Boolean> validateApiKey(String apiKey);

    Mono<Boolean> hasAvailableQuota(String apiKey);

    Mono<Boolean> canAccessModel(String apiKey, String model);

    java.util.List<String> getSupportedModelList(String apiKey);

    ApiKey updateQuota(Long id, java.math.BigDecimal maxAmount, Long maxRequests, Long maxConcurrentRequests, java.util.List<String> supportedModels, String keyGroup);

    ApiKey resetRequestLimit(Long id);

    Mono<Long> getMaxConcurrentRequests(String apiKey);
}
