package com.hospitalinfo.hospitalinformationsystem.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis 分布式锁
 * 基于 SETNX + 过期时间 + Lua 释放锁 实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisDistributedLock {

    private final RedisTemplate<String, Object> redisTemplate;

    /** 锁默认过期时间（秒），防止死锁 */
    private static final long DEFAULT_EXPIRE_SECONDS = 10;
    /** 获取锁默认重试次数 */
    private static final int DEFAULT_RETRY_COUNT = 3;
    /** 获取锁重试间隔（毫秒） */
    private static final long DEFAULT_RETRY_INTERVAL_MS = 100;

    /**
     * Lua脚本：释放锁（保证原子性，只有持有锁的线程才能释放）
     * KEYS[1] = lockKey, ARGV[1] = lockValue
     */
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "   return redis.call('del', KEYS[1]) " +
            "else " +
            "   return 0 " +
            "end";

    /**
     * 尝试获取分布式锁（使用默认重试策略）
     * @param lockKey 锁的Key
     * @return 锁的唯一标识（用于释放），获取失败返回null
     */
    public String tryLock(String lockKey) {
        return tryLock(lockKey, DEFAULT_EXPIRE_SECONDS, DEFAULT_RETRY_COUNT, DEFAULT_RETRY_INTERVAL_MS);
    }

    /**
     * 尝试获取分布式锁
     * @param lockKey 锁的Key
     * @param expireSeconds 锁过期时间（秒）
     * @param retryCount 重试次数
     * @param retryIntervalMs 重试间隔（毫秒）
     * @return 锁的唯一标识（用于释放），获取失败返回null
     */
    public String tryLock(String lockKey, long expireSeconds, int retryCount, long retryIntervalMs) {
        String lockValue = UUID.randomUUID().toString();

        for (int i = 0; i < retryCount; i++) {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, expireSeconds, TimeUnit.SECONDS);

            if (Boolean.TRUE.equals(acquired)) {
                log.debug("获取分布式锁成功 - key: {}, value: {}", lockKey, lockValue);
                return lockValue;
            }

            if (i < retryCount - 1) {
                try {
                    Thread.sleep(retryIntervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }

        log.warn("获取分布式锁失败 - key: {}, 重试{}次后仍无法获取", lockKey, retryCount);
        return null;
    }

    /**
     * 释放分布式锁（Lua脚本保证原子性）
     * @param lockKey 锁的Key
     * @param lockValue 获取锁时返回的唯一标识
     * @return 是否成功释放
     */
    public boolean unlock(String lockKey, String lockValue) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script,
                Collections.singletonList(lockKey), lockValue);

        boolean released = Long.valueOf(1L).equals(result);
        if (released) {
            log.debug("释放分布式锁成功 - key: {}", lockKey);
        } else {
            log.warn("释放分布式锁失败（锁已过期或被其他线程持有） - key: {}", lockKey);
        }
        return released;
    }
}
