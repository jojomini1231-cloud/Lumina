package com.lumina.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumina.entity.User;
import com.lumina.mapper.UserMapper;
import com.lumina.service.UserService;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lumina.dto.UserProfileUpdateDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public User updateProfile(String currentUsername, UserProfileUpdateDto updateDto) {
        // 获取当前用户
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, currentUsername));
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 更新用户名
        if (updateDto.getUsername() != null && !updateDto.getUsername().isBlank()) {
            if (!currentUsername.equals(updateDto.getUsername())) {
                // 检查新用户名是否已存在
                long count = this.count(new LambdaQueryWrapper<User>().eq(User::getUsername, updateDto.getUsername()));
                if (count > 0) {
                    throw new RuntimeException("用户名已存在");
                }
                user.setUsername(updateDto.getUsername());
            }
        }

        // 更新密码
        if (updateDto.getPassword() != null && !updateDto.getPassword().isBlank()) {
            // 验证原密码
            if (updateDto.getOriginalPassword() == null || !passwordEncoder.matches(updateDto.getOriginalPassword(), user.getPassword())) {
                throw new RuntimeException("原密码错误");
            }
            user.setPassword(passwordEncoder.encode(updateDto.getPassword()));
        }

        user.setUpdatedAt(LocalDateTime.now());
        this.updateById(user);
        
        // 返回前清除敏感信息
        user.setPassword(null);
        return user;
    }
}
