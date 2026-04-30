package com.hospitalinfo.hospitalinformationsystem.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Redis缓存配置
 * 使用Spring Cache + Redis实现声明式缓存
 */
@Slf4j
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
public class CacheConfig {

    /** 缓存名称常量 */
    public static final String CACHE_BILLING = "billing";
    public static final String CACHE_MEDICINE = "medicine";
    public static final String CACHE_DEPARTMENT = "department";
    public static final String CACHE_PATIENT = "patient";
    public static final String CACHE_SCHEDULE = "schedule";
    public static final String CACHE_DOCTOR = "doctor";

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        // 配置支持Java 8日期时间的ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // 默认缓存配置：30分钟过期
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        // 各缓存空间独立配置
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(CACHE_BILLING, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put(CACHE_MEDICINE, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put(CACHE_DEPARTMENT, defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put(CACHE_PATIENT, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put(CACHE_SCHEDULE, defaultConfig.entryTtl(Duration.ofMinutes(3)));
        cacheConfigurations.put(CACHE_DOCTOR, defaultConfig.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    /**
     * 包装 CacheManager，在每次缓存查询时打印命中/未命中日志
     */
    @Bean
    @Primary
    public CacheManager cacheManager(RedisCacheManager redisCacheManager) {
        return new CacheManager() {
            @Override
            public Cache getCache(String name) {
                Cache delegate = redisCacheManager.getCache(name);
                if (delegate == null) return null;
                return new LoggingCache(delegate);
            }

            @Override
            public Collection<String> getCacheNames() {
                return redisCacheManager.getCacheNames();
            }
        };
    }

    /**
     * 带日志输出的缓存装饰器
     */
    static class LoggingCache implements Cache {
        private final Cache delegate;

        LoggingCache(Cache delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public Object getNativeCache() {
            return delegate.getNativeCache();
        }

        @Override
        public ValueWrapper get(Object key) {
            long start = System.nanoTime();
            ValueWrapper result = delegate.get(key);
            long elapsed = (System.nanoTime() - start) / 1_000_000;
            String keyStr = key == null ? "null" : key.toString();
            if (result != null) {
                log.info("[缓存命中] cache={} key={} 耗时={}ms", getName(), keyStr, elapsed);
            } else {
                log.info("[缓存未命中] cache={} key={} → 查数据库 耗时={}ms", getName(), keyStr, elapsed);
            }
            return result;
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            long start = System.nanoTime();
            T result = delegate.get(key, type);
            long elapsed = (System.nanoTime() - start) / 1_000_000;
            String keyStr = key == null ? "null" : key.toString();
            if (result != null) {
                log.info("[缓存命中] cache={} key={} 耗时={}ms", getName(), keyStr, elapsed);
            } else {
                log.info("[缓存未命中] cache={} key={} → 查数据库 耗时={}ms", getName(), keyStr, elapsed);
            }
            return result;
        }

        @Override
        public <T> T get(Object key, Callable<T> valueLoader) {
            return delegate.get(key, valueLoader);
        }

        @Override
        public void put(Object key, Object value) {
            delegate.put(key, value);
        }

        @Override
        public ValueWrapper putIfAbsent(Object key, Object value) {
            return delegate.putIfAbsent(key, value);
        }

        @Override
        public void evict(Object key) {
            log.info("[缓存清除] cache={} key={}", getName(), key);
            delegate.evict(key);
        }

        @Override
        public boolean evictIfPresent(Object key) {
            boolean result = delegate.evictIfPresent(key);
            log.info("[缓存清除] cache={} key={} removed={}", getName(), key, result);
            return result;
        }

        @Override
        public void clear() {
            log.info("[缓存清空] cache={}", getName());
            delegate.clear();
        }

        @Override
        public boolean invalidate() {
            return delegate.invalidate();
        }
    }
}
