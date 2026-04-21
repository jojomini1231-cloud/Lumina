package com.lumina.service;

import com.lumina.config.LuminaProperties;
import com.lumina.dto.ModelGroupConfig;
import com.lumina.entity.LlmModel;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class HotPathCacheService {

    private static final Logger log = LoggerFactory.getLogger(HotPathCacheService.class);
    private static final String INVALIDATION_CHANNEL = "lumina:cache:invalidation";

    private final ConcurrentHashMap<String, CacheEntry<ModelGroupConfig>> groupConfigCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CacheEntry<Boolean>> apiKeyValidityCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CacheEntry<LlmModel>> modelPriceCache = new ConcurrentHashMap<>();

    private final long groupConfigTtlMs;
    private final long apiKeyTtlMs;
    private final long modelPriceTtlMs;
    private final MeterRegistry meterRegistry;
    private final StringRedisTemplate stringRedisTemplate;

    public HotPathCacheService(LuminaProperties properties, MeterRegistry meterRegistry, StringRedisTemplate stringRedisTemplate) {
        this.groupConfigTtlMs = properties.getCache().getGroupConfigTtlSeconds() * 1000L;
        this.apiKeyTtlMs = properties.getCache().getApiKeyTtlSeconds() * 1000L;
        this.modelPriceTtlMs = properties.getCache().getModelPriceTtlSeconds() * 1000L;
        this.meterRegistry = meterRegistry;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public ModelGroupConfig getCachedGroupConfig(String key) {
        return getIfPresent(groupConfigCache, key, "group_config");
    }

    public ModelGroupConfig getGroupConfig(String key, Supplier<ModelGroupConfig> loader) {
        return getOrLoad(groupConfigCache, key, groupConfigTtlMs, loader, "group_config");
    }

    public Boolean getCachedApiKeyValidity(String apiKey) {
        return getIfPresent(apiKeyValidityCache, apiKey, "api_key");
    }

    public Boolean getApiKeyValidity(String apiKey, Supplier<Boolean> loader) {
        return getOrLoad(apiKeyValidityCache, apiKey, apiKeyTtlMs, loader, "api_key");
    }

    public LlmModel getCachedModelPrice(String modelName) {
        return getIfPresent(modelPriceCache, modelName, "model_price");
    }

    public LlmModel getModelPrice(String modelName, Supplier<LlmModel> loader) {
        return getOrLoad(modelPriceCache, modelName, modelPriceTtlMs, loader, "model_price");
    }

    public void invalidateGroupConfig(String key) {
        stringRedisTemplate.convertAndSend(INVALIDATION_CHANNEL, "group_config:" + key);
    }

    public void invalidateAllGroupConfigs() {
        stringRedisTemplate.convertAndSend(INVALIDATION_CHANNEL, "group_config:ALL");
    }

    public void invalidateApiKey(String apiKey) {
        stringRedisTemplate.convertAndSend(INVALIDATION_CHANNEL, "api_key:" + apiKey);
    }

    public void invalidateAllApiKeys() {
        stringRedisTemplate.convertAndSend(INVALIDATION_CHANNEL, "api_key:ALL");
    }

    public void invalidateModelPrice(String modelName) {
        stringRedisTemplate.convertAndSend(INVALIDATION_CHANNEL, "model_price:" + modelName);
    }

    public void invalidateAllModelPrices() {
        stringRedisTemplate.convertAndSend(INVALIDATION_CHANNEL, "model_price:ALL");
    }

    public void receiveInvalidationMessage(String message) {
        if (message == null) return;
        String[] parts = message.split(":", 2);
        if (parts.length != 2) return;

        String cacheName = parts[0];
        String key = parts[1];

        log.debug("Received invalidation message for cache '{}', key '{}'", cacheName, key);

        switch (cacheName) {
            case "group_config":
                if ("ALL".equals(key)) {
                    groupConfigCache.clear();
                } else {
                    groupConfigCache.remove(key);
                }
                meterRegistry.counter("lumina_cache_invalidations_total", "cache", "group_config").increment();
                break;
            case "api_key":
                if ("ALL".equals(key)) {
                    apiKeyValidityCache.clear();
                } else {
                    apiKeyValidityCache.remove(key);
                }
                meterRegistry.counter("lumina_cache_invalidations_total", "cache", "api_key").increment();
                break;
            case "model_price":
                if ("ALL".equals(key)) {
                    modelPriceCache.clear();
                } else {
                    modelPriceCache.remove(key);
                }
                meterRegistry.counter("lumina_cache_invalidations_total", "cache", "model_price").increment();
                break;
        }
    }

    private <T> T getIfPresent(ConcurrentHashMap<String, CacheEntry<T>> cache, String key, String cacheName) {
        CacheEntry<T> entry = cache.get(key);
        if (entry == null) {
            meterRegistry.counter("lumina_cache_lookups_total", "cache", cacheName, "result", "miss").increment();
            return null;
        }
        if (entry.isExpired()) {
            cache.remove(key, entry);
            meterRegistry.counter("lumina_cache_lookups_total", "cache", cacheName, "result", "expired").increment();
            return null;
        }
        meterRegistry.counter("lumina_cache_lookups_total", "cache", cacheName, "result", "hit").increment();
        return entry.value();
    }

    private <T> T getOrLoad(
            ConcurrentHashMap<String, CacheEntry<T>> cache,
            String key,
            long ttlMs,
            Supplier<T> loader,
            String cacheName
    ) {
        T cached = getIfPresent(cache, key, cacheName);
        if (cached != null || cache.containsKey(key)) {
            return cached;
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        T loaded = loader.get();
        sample.stop(meterRegistry.timer("lumina_cache_load_duration", "cache", cacheName));
        if (loaded == null) {
            cache.remove(key);
            meterRegistry.counter("lumina_cache_loads_total", "cache", cacheName, "result", "null").increment();
            return null;
        }

        cache.put(key, new CacheEntry<>(loaded, System.currentTimeMillis() + ttlMs));
        meterRegistry.counter("lumina_cache_loads_total", "cache", cacheName, "result", "loaded").increment();
        return loaded;
    }

    private record CacheEntry<T>(T value, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() >= expiresAt;
        }
    }
}
