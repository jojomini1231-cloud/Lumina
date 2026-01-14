package com.lumina.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumina.dto.ModelGroupConfig;
import com.lumina.entity.Group;
import com.lumina.entity.GroupItem;
import com.lumina.mapper.GroupMapper;
import com.lumina.service.GroupItemService;
import com.lumina.service.GroupService;
import com.lumina.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class GroupServiceImpl extends ServiceImpl<GroupMapper, Group> implements GroupService {

    @Autowired
    private RedisService redisService;

    @Autowired
    private GroupItemService groupItemService;

    @Override
    public Page<Group> getGroupsByPage(Page<Object> page) {
        return baseMapper.getGroupsByPage(page);
    }

    @Override
    public Group getGroupById(Long id) {
        Group group = getById(id);
        if (Objects.isNull(group)) {
            throw new IllegalArgumentException("Group not found with id: " + id);
        }
        group.setGroupItems(groupItemService.list(new LambdaQueryWrapper<GroupItem>()
                .eq(GroupItem::getGroupId, id)));
        return group;
    }

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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createGroup(Group group) {
        if (Objects.isNull(group.getGroupItems()) || group.getGroupItems().isEmpty()) {
            throw new IllegalArgumentException("Group items cannot be empty");
        }
        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());
        boolean success = save(group);
        if (!success) {
            throw new IllegalArgumentException("Failed to create group");
        }
        group.getGroupItems().forEach(item -> item.setGroupId(group.getId()));
        groupItemService.saveBatch(group.getGroupItems());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateGroup(Long id,Group group) {
        group.setId(id);
        group.setUpdatedAt(LocalDateTime.now());
        boolean success = updateById(group);
        if (!success) {
            throw new IllegalArgumentException("Group not found with id: " + id);
        }
        // 删除旧的关联
        groupItemService.remove(new LambdaQueryWrapper<GroupItem>()
                .eq(GroupItem::getGroupId, id));
        // 批量插入新的关联
        group.getGroupItems().forEach(item -> item.setGroupId(group.getId()));
        groupItemService.saveBatch(group.getGroupItems());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteGroup(Long id) {
        removeById(id);
        groupItemService.remove(new LambdaQueryWrapper<GroupItem>()
                .eq(GroupItem::getGroupId, id));
    }
}
