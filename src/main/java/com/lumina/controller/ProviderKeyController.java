package com.lumina.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumina.dto.ApiResponse;
import com.lumina.entity.ProviderKey;
import com.lumina.service.ProviderKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/provider-keys")
public class ProviderKeyController {

    @Autowired
    private ProviderKeyService providerKeyService;

    @GetMapping
    public ApiResponse<List<ProviderKey>> getAllProviderKeys() {
        List<ProviderKey> providerKeys = providerKeyService.list();
        return ApiResponse.success(providerKeys);
    }

    @GetMapping("/{id}")
    public ApiResponse<ProviderKey> getProviderKeyById(@PathVariable Long id) {
        ProviderKey providerKey = providerKeyService.getById(id);
        if (providerKey == null) {
            throw new IllegalArgumentException("ProviderKey not found with id: " + id);
        }
        return ApiResponse.success(providerKey);
    }

    @PostMapping
    public ApiResponse<ProviderKey> createProviderKey(@RequestBody ProviderKey providerKey) {
        providerKey.setCreatedAt(LocalDateTime.now());
        providerKey.setUpdatedAt(LocalDateTime.now());
        boolean success = providerKeyService.save(providerKey);
        if (!success) {
            throw new IllegalArgumentException("Failed to create provider key");
        }
        return ApiResponse.success(providerKey);
    }

    @PutMapping("/{id}")
    public ApiResponse<ProviderKey> updateProviderKey(@PathVariable Long id, @RequestBody ProviderKey providerKey) {
        providerKey.setId(id);
        providerKey.setUpdatedAt(LocalDateTime.now());
        boolean success = providerKeyService.updateById(providerKey);
        if (!success) {
            throw new IllegalArgumentException("ProviderKey not found with id: " + id);
        }
        return ApiResponse.success(providerKey);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProviderKey(@PathVariable Long id) {
        boolean success = providerKeyService.removeById(id);
        if (!success) {
            throw new IllegalArgumentException("ProviderKey not found with id: " + id);
        }
        return ApiResponse.success(null);
    }

    @GetMapping("/page")
    public ApiResponse<Page<ProviderKey>> getProviderKeysByPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<ProviderKey> page = providerKeyService.page(new Page<>(current, size));
        return ApiResponse.success(page);
    }

    @GetMapping("/provider/{providerId}")
    public ApiResponse<List<ProviderKey>> getProviderKeysByProviderId(@PathVariable Long providerId) {
        QueryWrapper<ProviderKey> wrapper = new QueryWrapper<>();
        wrapper.eq("provider_id", providerId);
        List<ProviderKey> providerKeys = providerKeyService.list(wrapper);
        return ApiResponse.success(providerKeys);
    }

    @GetMapping("/enabled")
    public ApiResponse<List<ProviderKey>> getEnabledProviderKeys() {
        QueryWrapper<ProviderKey> wrapper = new QueryWrapper<>();
        wrapper.eq("is_enabled", true);
        List<ProviderKey> providerKeys = providerKeyService.list(wrapper);
        return ApiResponse.success(providerKeys);
    }
}
