package com.hospitalinfo.hospitalinformationsystem.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka配置类
 * 配置消息主题和消费者
 * 只有在 kafka.enabled=true 时才创建主题
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaConfig {

    @Value("${kafka.topics.ai-task:his-ai-task}")
    private String aiTaskTopic;

    @Value("${kafka.topics.appointment-notify:his-appointment-notify}")
    private String appointmentNotifyTopic;

    /**
     * AI任务主题
     */
    @Bean
    public NewTopic aiTaskTopic() {
        log.info("创建Kafka主题: {}", aiTaskTopic);
        return TopicBuilder.name(aiTaskTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * 预约通知主题
     */
    @Bean
    public NewTopic appointmentNotifyTopic() {
        log.info("创建Kafka主题: {}", appointmentNotifyTopic);
        return TopicBuilder.name(appointmentNotifyTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
