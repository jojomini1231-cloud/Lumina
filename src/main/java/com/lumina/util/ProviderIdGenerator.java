package com.lumina.util;

import com.lumina.dto.ModelGroupConfigItem;

public final class ProviderIdGenerator {

    private ProviderIdGenerator() {
    }

    /**
     * 生成 Provider 唯一标识，用于运行时状态注册与 failover 去重。
     * 格式必须保持稳定：baseUrl_apiKeyHash_modelName。
     */
    public static String generate(ModelGroupConfigItem item) {
        return String.format("%s_%s_%s",
                item.getBaseUrl(),
                item.getApiKey() != null ? item.getApiKey().hashCode() : "null",
                item.getModelName());
    }
}
