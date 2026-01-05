package com.example.demo.infra;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisLock {

    private final StringRedisTemplate redis;

    public RedisLock(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean lock(String key, Duration ttl) {
        return Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(key, "LOCK", ttl)
        );
    }

    public void unlock(String key) {
        redis.delete(key);
    }
}
