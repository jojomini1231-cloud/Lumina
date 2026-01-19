package com.lumina.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumina.entity.ApiKey;
import com.lumina.mapper.ApiKeyMapper;
import com.lumina.service.ApiKeyService;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ApiKeyServiceImpl extends ServiceImpl<ApiKeyMapper, ApiKey> implements ApiKeyService {

    @Override
    public ApiKey generateApiKey(String name) {
        ApiKey apiKey = new ApiKey();
        apiKey.setName(name);
        apiKey.setApiKey("sk-" + UUID.randomUUID().toString().replace("-", ""));
        apiKey.setIsEnabled(true);
        apiKey.setCreatedAt(LocalDateTime.now());
        apiKey.setUpdatedAt(LocalDateTime.now());
        this.save(apiKey);
        return apiKey;
    }
}
