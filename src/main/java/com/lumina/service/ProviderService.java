package com.lumina.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lumina.entity.Provider;

import java.util.List;

public interface ProviderService extends IService<Provider> {

    /**
     * 获取模型列表
     * @param provider
     * @return
     */
    List<String> getModels(Provider provider);
}
