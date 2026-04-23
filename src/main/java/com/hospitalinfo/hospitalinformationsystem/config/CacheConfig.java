package com.hospitalinfo.hospitalinformationsystem.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis缓存配置
 * 使用Spring Cache + Redis实现声明式缓存
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** 缓存名称常量 */
    public static final String CACHE_BILLING = "billing";
    public static final String CACHE_MEDICINE = "medicine";
    public static final String CACHE_DEPARTMENT = "department";
    public static final String CACHE_PATIENT = "patient";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 默认缓存配置：30分钟过期
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // 各缓存空间独立配置
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        // 费用数据：5分钟（变动较频繁）
        cacheConfigurations.put(CACHE_BILLING, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        // 药品数据：30分钟（相对稳定）
        cacheConfigurations.put(CACHE_MEDICINE, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        // 科室数据：1小时（极少变动）
        cacheConfigurations.put(CACHE_DEPARTMENT, defaultConfig.entryTtl(Duration.ofHours(1)));
        // 患者信息：10分钟
        cacheConfigurations.put(CACHE_PATIENT, defaultConfig.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
