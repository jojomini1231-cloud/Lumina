package com.lumina.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserProfileUpdateDto {
    @Size(min = 3, max = 50, message = "用户名长度必须在3到50个字符之间")
    private String username;

    @Size(min = 6, max = 100, message = "密码长度必须在6到100个字符之间")
    private String password;

    private String originalPassword;
}
