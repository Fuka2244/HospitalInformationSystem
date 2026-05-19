package com.hospitalinfo.hospitalinformationsystem.service.impl;

import com.hospitalinfo.hospitalinformationsystem.service.IKafkaAiProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka AI消息生产者服务实现
 * 将AI任务异步发送到Kafka消息队列
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaAiProducerServiceImpl implements IKafkaAiProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topics.ai-task:his-ai-task}")
    private String aiTaskTopic;

    @Override
    public boolean submitAiRecommendTask(String taskId, String patientId, String symptom) {
        String message = buildMessage("APPOINTMENT_RECOMMEND", taskId, patientId, symptom, null, null, null);
        return sendMessage(aiTaskTopic, taskId, message);
    }

    @Override
    public boolean submitAiTriageTask(String taskId, String patientId, String message, String historyJson) {
        String msg = buildMessage("APPOINTMENT_TRIAGE", taskId, patientId, null, message, historyJson, null);
        return sendMessage(aiTaskTopic, taskId, msg);
    }

    @Override
    public boolean submitAiMedicineRecommendTask(String taskId, String patientId, String symptom) {
        String message = buildMessage("MEDICINE_RECOMMEND", taskId, patientId, symptom, null, null, null);
        return sendMessage(aiTaskTopic, taskId, message);
    }

    @Override
    public boolean submitAiMedicineChatTask(String taskId, String patientId, String message, String historyJson) {
        String msg = buildMessage("MEDICINE_CHAT", taskId, patientId, null, message, historyJson, null);
        return sendMessage(aiTaskTopic, taskId, msg);
    }

    @Override
    public boolean submitAiReportTask(String taskId, String patientId, Long recordId) {
        String message = buildMessage("REPORT_GENERATE", taskId, patientId, null, null, null, recordId);
        return sendMessage(aiTaskTopic, taskId, message);
    }

    /**
     * 构建消息JSON
     */
    private String buildMessage(String taskType, String taskId, String patientId,
                                 String symptom, String message, String historyJson, Long recordId) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"taskType\":\"").append(taskType).append("\",");
        sb.append("\"taskId\":\"").append(taskId).append("\",");
        sb.append("\"patientId\":\"").append(nullToEmpty(patientId)).append("\",");
        if (symptom != null) {
            sb.append("\"symptom\":\"").append(escapeJson(symptom)).append("\",");
        }
        if (message != null) {
            sb.append("\"message\":\"").append(escapeJson(message)).append("\",");
        }
        if (historyJson != null) {
            sb.append("\"historyJson\":\"").append(escapeJson(historyJson)).append("\",");
        }
        if (recordId != null) {
            sb.append("\"recordId\":").append(recordId).append(",");
        }
        sb.append("\"timestamp\":").append(System.currentTimeMillis());
        sb.append("}");
        return sb.toString();
    }

    /**
     * 发送消息到Kafka
     */
    private boolean sendMessage(String topic, String key, String message) {
        try {
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, key, message);
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("发送Kafka消息失败: topic={}, key={}, error={}", topic, key, ex.getMessage());
                } else {
                    log.info("发送Kafka消息成功: topic={}, key={}, partition={}, offset={}",
                            topic, key,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });
            return true;
        } catch (Exception e) {
            log.error("发送Kafka消息异常: topic={}, key={}, error={}", topic, key, e.getMessage());
            return false;
        }
    }

    private String nullToEmpty(String str) {
        return str == null ? "" : str;
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
