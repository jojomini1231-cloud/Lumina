package com.lumina.controller;

import com.lumina.dto.ApiResponse;
import com.lumina.dto.UserProfileUpdateDto;
import com.lumina.entity.User;
import com.lumina.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/v1/user")
@Validated
public class UserProfileController {

    @Autowired
    private UserService userService;

    @PutMapping("/profile")
    public Mono<ApiResponse<User>> updateProfile(@Valid @RequestBody UserProfileUpdateDto updateDto) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .flatMap(username -> Mono.fromCallable(() -> userService.updateProfile(username, updateDto)))
                .subscribeOn(Schedulers.boundedElastic())
                .map(user -> ApiResponse.success("用户信息更新成功", user))
                .onErrorResume(e -> Mono.just(ApiResponse.error(400, e.getMessage())));
    }
}