package com.lumina.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisObjectTemplate;

    private static final String GROUP_PREFIX = "group:";
    private static final long DEFAULT_EXPIRE = 3600; // 1小时

    public <T> T get(String key, Class<T> clazz) {
        Object value = redisObjectTemplate.opsForValue().get(GROUP_PREFIX + key);
        return value != null ? clazz.cast(value) : null;
    }

    public void set(String key, Object value) {
        redisObjectTemplate.opsForValue().set(GROUP_PREFIX + key, value, DEFAULT_EXPIRE, TimeUnit.SECONDS);
    }

    public void delete(String key) {
        redisObjectTemplate.delete(GROUP_PREFIX + key);
    }
}
