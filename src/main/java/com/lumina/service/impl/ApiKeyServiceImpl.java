package com.lumina.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumina.dto.ApiKeyUsageDto;
import com.lumina.entity.ApiKey;
import com.lumina.mapper.ApiKeyMapper;
import com.lumina.service.ApiKeyService;
import com.lumina.service.HotPathCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ApiKeyServiceImpl extends ServiceImpl<ApiKeyMapper, ApiKey> implements ApiKeyService {
    private static final String DEFAULT_KEY_GROUP = "自用";

    @Autowired
    private HotPathCacheService hotPathCacheService;

    @Override
    public ApiKey generateApiKey(String name) {
        return generateApiKey(name, DEFAULT_KEY_GROUP);
    }

    @Override
    public ApiKey generateApiKey(String name, String keyGroup) {
        ApiKey apiKey = new ApiKey();
        apiKey.setName(name);
        apiKey.setKeyGroup(normalizeKeyGroup(keyGroup));
        apiKey.setApiKey("sk-" + UUID.randomUUID().toString().replace("-", ""));
        apiKey.setIsEnabled(true);
        apiKey.setCreatedAt(LocalDateTime.now());
        apiKey.setUpdatedAt(LocalDateTime.now());
        this.save(apiKey);
        hotPathCacheService.invalidateApiKey(apiKey.getApiKey());
        return apiKey;
    }

    @Override
    public Mono<Boolean> validateApiKey(String apiKey) {
        Boolean cached = hotPathCacheService.getCachedApiKeyValidity(apiKey);
        if (cached != null) {
            return Mono.just(cached);
        }

        return Mono.fromCallable(() -> hotPathCacheService.getApiKeyValidity(apiKey, () -> {
            LambdaQueryWrapper<ApiKey> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ApiKey::getApiKey, apiKey)
                    .eq(ApiKey::getIsEnabled, true);
            ApiKey key = this.getOne(queryWrapper);
            if (key == null) {
                return false;
            }
            if (key.getExpiredAt() != null && key.getExpiredAt() > 0) {
                return System.currentTimeMillis() / 1000 < key.getExpiredAt();
            }
            return true;
        })).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Boolean> hasAvailableQuota(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return Mono.just(false);
        }

        return Mono.fromCallable(() -> {
            ApiKeyUsageDto usage = baseMapper.selectApiKeyUsageByKey(apiKey);
            if (usage == null) {
                return false;
            }

            BigDecimal maxAmount = usage.getMaxAmount();
            if (maxAmount != null) {
                BigDecimal totalCost = usage.getTotalCost() != null ? usage.getTotalCost() : BigDecimal.ZERO;
                if (totalCost.compareTo(maxAmount) >= 0) {
                    return false;
                }
            }

            Long maxRequests = usage.getMaxRequests();
            if (maxRequests == null) {
                return true;
            }

            long requestLimitUsed = usage.getRequestLimitUsed() != null ? usage.getRequestLimitUsed() : 0L;
            return requestLimitUsed < maxRequests;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Boolean> canAccessModel(String apiKey, String model) {
        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(model)) {
            return Mono.just(false);
        }

        return Mono.fromCallable(() -> {
            LambdaQueryWrapper<ApiKey> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ApiKey::getApiKey, apiKey)
                    .eq(ApiKey::getIsEnabled, true);
            ApiKey key = this.getOne(queryWrapper);
            if (key == null) {
                return false;
            }
            List<String> supportedModels = parseSupportedModels(key.getSupportedModels());
            return supportedModels.isEmpty() || supportedModels.contains(model);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public List<String> getSupportedModelList(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<ApiKey> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApiKey::getApiKey, apiKey)
                .eq(ApiKey::getIsEnabled, true);
        ApiKey key = this.getOne(queryWrapper);
        if (key == null) {
            return Collections.emptyList();
        }
        return parseSupportedModels(key.getSupportedModels());
    }

    @Override
    public ApiKey updateQuota(Long id, BigDecimal maxAmount, Long maxRequests, Long maxConcurrentRequests, List<String> supportedModels, String keyGroup) {
        if (maxAmount != null && maxAmount.signum() < 0) {
            throw new IllegalArgumentException("Max amount must be greater than or equal to 0");
        }
        if (maxRequests != null && maxRequests < 0) {
            throw new IllegalArgumentException("Max requests must be greater than or equal to 0");
        }
        if (maxConcurrentRequests != null && maxConcurrentRequests < 0) {
            throw new IllegalArgumentException("Max concurrent requests must be greater than or equal to 0");
        }

        ApiKey existing = this.getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("ApiKey not found with id: " + id);
        }

        LambdaUpdateWrapper<ApiKey> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ApiKey::getId, id)
                .set(ApiKey::getMaxAmount, maxAmount)
                .set(ApiKey::getMaxRequests, maxRequests)
                .set(ApiKey::getMaxConcurrentRequests, maxConcurrentRequests)
                .set(ApiKey::getSupportedModels, serializeSupportedModels(supportedModels))
                .set(ApiKey::getKeyGroup, normalizeKeyGroup(keyGroup))
                .set(ApiKey::getUpdatedAt, LocalDateTime.now());

        boolean updated = super.update(wrapper);
        if (!updated) {
            throw new IllegalArgumentException("Failed to update api key quota");
        }
        hotPathCacheService.invalidateAllApiKeys();
        return this.getById(id);
    }

    @Override
    public Mono<Long> getMaxConcurrentRequests(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return Mono.just(0L);
        }

        return Mono.fromCallable(() -> {
            LambdaQueryWrapper<ApiKey> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ApiKey::getApiKey, apiKey)
                    .eq(ApiKey::getIsEnabled, true);
            ApiKey key = this.getOne(queryWrapper);
            if (key == null || key.getMaxConcurrentRequests() == null) {
                return 0L;
            }
            return Math.max(0L, key.getMaxConcurrentRequests());
        }).subscribeOn(Schedulers.boundedElastic());
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

    private String serializeSupportedModels(List<String> supportedModels) {
        if (supportedModels == null || supportedModels.isEmpty()) {
            return null;
        }
        String value = supportedModels.stream()
                .map(model -> model == null ? "" : model.trim())
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining(","));
        return value.isEmpty() ? null : value;
    }

    private String normalizeKeyGroup(String keyGroup) {
        return StringUtils.hasText(keyGroup) ? keyGroup.trim() : DEFAULT_KEY_GROUP;
    }

    @Override
    public ApiKey resetRequestLimit(Long id) {
        ApiKey existing = this.getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("ApiKey not found with id: " + id);
        }

        LambdaUpdateWrapper<ApiKey> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ApiKey::getId, id)
                .set(ApiKey::getRequestLimitResetAt, System.currentTimeMillis() / 1000)
                .set(ApiKey::getUpdatedAt, LocalDateTime.now());

        boolean updated = super.update(wrapper);
        if (!updated) {
            throw new IllegalArgumentException("Failed to reset api key request limit");
        }
        hotPathCacheService.invalidateAllApiKeys();
        return this.getById(id);
    }

    @Override
    public boolean save(ApiKey entity) {
        boolean saved = super.save(entity);
        if (saved) {
            hotPathCacheService.invalidateAllApiKeys();
        }
        return saved;
    }

    @Override
    public boolean updateById(ApiKey entity) {
        boolean updated = super.updateById(entity);
        if (updated) {
            hotPathCacheService.invalidateAllApiKeys();
        }
        return updated;
    }

    @Override
    public boolean removeById(Serializable id) {
        boolean removed = super.removeById(id);
        if (removed) {
            hotPathCacheService.invalidateAllApiKeys();
        }
        return removed;
    }
}
