package com.lumina.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lumina.dto.ModelGroupConfig;
import com.lumina.entity.Group;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GroupMapper extends BaseMapper<Group> {
    /**
     * 根据名称获取模型分组
     * @param modelGroupName
     * @return
     */
    ModelGroupConfig getModelGroupByName(String modelGroupName);
}
