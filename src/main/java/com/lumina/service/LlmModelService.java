package com.lumina.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lumina.entity.LlmModel;

import java.util.List;

public interface LlmModelService extends IService<LlmModel> {
    void syncModels();

    LlmModel findLatestByModelName(String modelName);

    /**
     * 设置某模型使用哪个上游供应商的价格进行计费
     * @param modelName 模型名称
     * @param provider 上游供应商名称
     */
    void setActiveProvider(String modelName, String provider);

    /**
     * 获取某模型的所有上游供应商记录
     */
    List<LlmModel> findProvidersByModelName(String modelName);

    /**
     * 分页查询
     */
    Page<LlmModel> queryPage(Page<Object> page, LambdaQueryWrapper<LlmModel> queryWrapper);
}
