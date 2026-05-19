package com.hospitalinfo.hospitalinformationsystem.service.impl;

import com.hospitalinfo.hospitalinformationsystem.service.IKafkaAiProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Kafka AI消息生产者降级实现
 * 当Kafka未启用时，提供降级处理（直接调用同步AI服务）
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "false", matchIfMissing = true)
public class KafkaAiProducerFallbackImpl implements IKafkaAiProducerService {

    @Override
    public boolean submitAiRecommendTask(String taskId, String patientId, String symptom) {
        log.warn("Kafka未启用，跳过AI推荐任务提交: taskId={}", taskId);
        return false;
    }

    @Override
    public boolean submitAiTriageTask(String taskId, String patientId, String message, String historyJson) {
        log.warn("Kafka未启用，跳过AI分诊任务提交: taskId={}", taskId);
        return false;
    }

    @Override
    public boolean submitAiMedicineRecommendTask(String taskId, String patientId, String symptom) {
        log.warn("Kafka未启用，跳过AI药品推荐任务提交: taskId={}", taskId);
        return false;
    }

    @Override
    public boolean submitAiMedicineChatTask(String taskId, String patientId, String message, String historyJson) {
        log.warn("Kafka未启用，跳过AI药品对话任务提交: taskId={}", taskId);
        return false;
    }

    @Override
    public boolean submitAiReportTask(String taskId, String patientId, Long recordId) {
        log.warn("Kafka未启用，跳过AI报告生成任务提交: taskId={}", taskId);
        return false;
    }
}
