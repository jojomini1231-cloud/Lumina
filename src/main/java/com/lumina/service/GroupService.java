package com.lumina.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lumina.dto.ModelGroupConfig;
import com.lumina.entity.Group;

public interface GroupService extends IService<Group> {

    /**
     * 根据模型名称获取模型分组
     * @param model
     * @return
     */
    ModelGroupConfig getModelGroupConfig(String model);
}
