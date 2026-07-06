package com.lumina.controller;

import com.lumina.dto.ApiKeyUsageDto;
import com.lumina.dto.ApiResponse;
import com.lumina.dto.PublicApiKeyUsageDto;
import com.lumina.dto.PublicApiKeyUsageRequest;
import com.lumina.mapper.ApiKeyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/public")
public class PublicApiKeyController {

    @Autowired
    private ApiKeyMapper apiKeyMapper;

    @PostMapping("/key-usage")
    public ApiResponse<PublicApiKeyUsageDto> getKeyUsage(@RequestBody PublicApiKeyUsageRequest request) {
        if (request == null || !StringUtils.hasText(request.getApiKey())) {
            throw new IllegalArgumentException("密钥不能为空");
        }

        ApiKeyUsageDto usage = apiKeyMapper.selectApiKeyUsageByKey(request.getApiKey().trim());
        if (usage == null) {
            throw new IllegalArgumentException("密钥不存在");
        }

        return ApiResponse.success(buildUsageResponse(usage));
    }

    private PublicApiKeyUsageDto buildUsageResponse(ApiKeyUsageDto usage) {
        boolean enabled = Boolean.TRUE.equals(usage.getIsEnabled());
        boolean expired = isExpired(usage.getExpiredAt());
        long usedRequests = usage.getRequestLimitUsed() != null ? usage.getRequestLimitUsed() : 0L;
        Long maxRequests = usage.getMaxRequests();
        boolean unlimitedRequests = maxRequests == null;
        Long availableRequests = unlimitedRequests ? null : Math.max(0L, maxRequests - usedRequests);
        String status = resolveStatus(enabled, expired, unlimitedRequests, availableRequests);
        List<String> supportedModelNames = parseSupportedModels(usage.getSupportedModels());

        return PublicApiKeyUsageDto.builder()
                .name(usage.getName())
                .enabled(enabled)
                .expired(expired)
                .status(status)
                .expiredAt(usage.getExpiredAt())
                .maxRequests(maxRequests)
                .usedRequests(usedRequests)
                .availableRequests(enabled && !expired ? availableRequests : 0L)
                .unlimitedRequests(unlimitedRequests)
                .totalRequests(defaultLong(usage.getTotalRequests()))
                .successRequests(defaultLong(usage.getSuccessRequests()))
                .totalCost(usage.getTotalCost() != null ? usage.getTotalCost() : BigDecimal.ZERO)
                .modelScope(supportedModelNames.isEmpty() ? "all" : "limited")
                .build();
    }

    private String resolveStatus(boolean enabled, boolean expired, boolean unlimitedRequests, Long availableRequests) {
        if (!enabled) {
            return "disabled";
        }
        if (expired) {
            return "expired";
        }
        if (!unlimitedRequests && availableRequests != null && availableRequests <= 0) {
            return "exhausted";
        }
        return "active";
    }

    private boolean isExpired(Long expiredAt) {
        return expiredAt != null && expiredAt > 0 && System.currentTimeMillis() / 1000 >= expiredAt;
    }

    private Long defaultLong(Long value) {
        return value != null ? value : 0L;
    }

    private List<String> parseSupportedModels(String supportedModels) {
        if (!StringUtils.hasText(supportedModels)) {
            return Collections.emptyList();
        }
        return Arrays.stream(supportedModels.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }
}
