package com.lumina.service;

import com.lumina.dto.UserProfileUpdateDto;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lumina.entity.User;

public interface UserService extends IService<User> {
    User updateProfile(String currentUsername, UserProfileUpdateDto dto);
}
