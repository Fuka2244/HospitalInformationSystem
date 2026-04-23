package com.hospitalinfo.hospitalinformationsystem.config;

import com.hospitalinfo.hospitalinformationsystem.service.AsyncTaskConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

import java.time.Duration;

/**
 * Redis Stream 消息队列配置
 * 用于AI请求的异步处理
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisStreamConfig {

    public static final String STREAM_AI_TASK = "stream:ai:task";
    public static final String GROUP_AI_CONSUMER = "group:ai:consumer";

    private final AsyncTaskConsumer asyncTaskConsumer;

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {

        var options = StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                .pollTimeout(Duration.ofSeconds(1))
                .build();

        var container = StreamMessageListenerContainer.create(connectionFactory, options);

        // 创建消费者组（如果不存在）
        try {
            var stringRedisTemplate = new RedisTemplate<String, String>();
            stringRedisTemplate.setConnectionFactory(connectionFactory);
            stringRedisTemplate.setKeySerializer(new StringRedisSerializer());
            stringRedisTemplate.setValueSerializer(new StringRedisSerializer());
            stringRedisTemplate.afterPropertiesSet();

            try {
                stringRedisTemplate.opsForStream().createGroup(STREAM_AI_TASK, GROUP_AI_CONSUMER);
                log.info("创建Redis Stream消费者组: {}", GROUP_AI_CONSUMER);
            } catch (Exception e) {
                log.info("消费者组已存在: {}", GROUP_AI_CONSUMER);
            }
        } catch (Exception e) {
            log.warn("初始化消费者组失败: {}", e.getMessage());
        }

        // 注册消费者
        Subscription subscription = container.receiveAutoAck(
                Consumer.from(GROUP_AI_CONSUMER, "consumer-" + System.currentTimeMillis()),
                StreamOffset.create(STREAM_AI_TASK, ReadOffset.lastConsumed()),
                asyncTaskConsumer
        );

        container.start();
        log.info("Redis Stream消息监听容器已启动");

        return container;
    }
}
