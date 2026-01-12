package com.lumina.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumina.dto.ApiResponse;
import com.lumina.entity.Provider;
import com.lumina.service.ProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/providers")
public class ProviderController {

    @Autowired
    private ProviderService providerService;

    @GetMapping
    public ApiResponse<List<Provider>> getAllProviders() {
        return ApiResponse.success(providerService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<Provider> getProviderById(@PathVariable Long id) {
        Provider provider = providerService.getById(id);
        if (provider == null) {
            throw new IllegalArgumentException("供应商不存在");
        }
        return ApiResponse.success(provider);
    }

    @PostMapping
    public ApiResponse<Provider> createProvider(@RequestBody Provider provider) {
        provider.setCreatedAt(LocalDateTime.now());
        provider.setUpdatedAt(LocalDateTime.now());
        providerService.save(provider);
        return ApiResponse.success("供应商创建成功", provider);
    }

    @PutMapping("/{id}")
    public ApiResponse<Provider> updateProvider(@PathVariable Long id, @RequestBody Provider provider) {
        provider.setId(id);
        provider.setUpdatedAt(LocalDateTime.now());
        providerService.updateById(provider);
        return ApiResponse.success("供应商更新成功", provider);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProvider(@PathVariable Long id) {
        providerService.removeById(id);
        return ApiResponse.success("供应商删除成功", null);
    }

    @GetMapping("/page")
    public ApiResponse<Page<Provider>> getProvidersByPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        return ApiResponse.success(providerService.page(new Page<>(current, size)));
    }

    @GetMapping("/enabled")
    public ApiResponse<List<Provider>> getEnabledProviders() {
        QueryWrapper<Provider> wrapper = new QueryWrapper<>();
        wrapper.eq("is_enabled", true);
        return ApiResponse.success(providerService.list(wrapper));
    }

    @GetMapping("/type/{type}")
    public ApiResponse<List<Provider>> getProvidersByType(@PathVariable Integer type) {
        QueryWrapper<Provider> wrapper = new QueryWrapper<>();
        wrapper.eq("type", type);
        return ApiResponse.success(providerService.list(wrapper));
    }
}
