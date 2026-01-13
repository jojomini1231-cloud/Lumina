package com.lumina.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumina.dto.ModelGroupConfig;
import com.lumina.entity.Group;
import com.lumina.mapper.GroupMapper;
import com.lumina.service.GroupService;
import com.lumina.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GroupServiceImpl extends ServiceImpl<GroupMapper, Group> implements GroupService {

    @Autowired
    private RedisService redisService;

    @Override
    public ModelGroupConfig getModelGroupConfig(String modelGroupName) {
        return baseMapper.getModelGroupByName(modelGroupName);
//        // 先查询redis缓存
//        ModelGroupConfig config = redisService.get(modelGroupName, ModelGroupConfig.class);
//        if (config != null) {
//            return config;
//        }
//
//        // 缓存未命中，查询数据库
//        config = baseMapper.getModelGroupByName(modelGroupName);
//        if (config != null) {
//            // 将结果缓存到redis
//            redisService.set(modelGroupName, config);
//        }
//        return config;
    }
}
