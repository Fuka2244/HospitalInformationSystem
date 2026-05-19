package com.hospitalinfo.hospitalinformationsystem.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalinfo.hospitalinformationsystem.ai.AiAppointmentService;
import com.hospitalinfo.hospitalinformationsystem.ai.AiBillingService;
import com.hospitalinfo.hospitalinformationsystem.ai.AiMedicineService;
import com.hospitalinfo.hospitalinformationsystem.dto.*;
import com.hospitalinfo.hospitalinformationsystem.entity.Billing;
import com.hospitalinfo.hospitalinformationsystem.mapper.BillingMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Kafka AI消息消费者
 * 异步处理来自Kafka消息队列的AI任务
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaAiConsumer {

    private final IAsyncTaskService asyncTaskService;
    private final AiAppointmentService aiAppointmentService;
    private final AiMedicineService aiMedicineService;
    private final AiBillingService aiBillingService;
    private final BillingMapper billingMapper;
    private final ObjectMapper objectMapper;

    /**
     * 监听AI任务主题，处理所有AI异步任务
     */
    @KafkaListener(topics = "${kafka.topics.ai-task:his-ai-task}",
                   groupId = "${spring.kafka.consumer.group-id:his-ai-consumer}")
    public void consumeAiTask(ConsumerRecord<String, String> record) {
        String key = record.key();
        String value = record.value();

        log.info("Kafka收到AI任务: key={}, partition={}, offset={}",
                key, record.partition(), record.offset());

        try {
            // 解析消息
            JsonNode json = objectMapper.readTree(value);
            String taskType = json.has("taskType") ? json.get("taskType").asText() : null;
            String taskId = json.has("taskId") ? json.get("taskId").asText() : key;
            String patientId = json.has("patientId") ? json.get("patientId").asText() : null;

            if (taskType == null) {
                log.warn("任务消息缺少taskType字段: {}", value);
                return;
            }

            // 更新状态为处理中
            asyncTaskService.saveTaskResult(AsyncTaskResult.processing(taskId));

            // 根据任务类型分发处理
            switch (taskType) {
                case "APPOINTMENT_RECOMMEND" -> handleAppointmentRecommend(taskId, patientId, json);
                case "APPOINTMENT_TRIAGE" -> handleAppointmentTriage(taskId, patientId, json);
                case "MEDICINE_RECOMMEND" -> handleMedicineRecommend(taskId, json);
                case "MEDICINE_CHAT" -> handleMedicineChat(taskId, json);
                case "BILLING_CHAT" -> handleBillingChat(taskId, patientId, json);
                case "BILLING_EXPLAIN" -> handleBillingExplain(taskId, patientId, json);
                case "REPORT_GENERATE" -> handleReportGenerate(taskId, json);
                default -> {
                    log.warn("未知任务类型: {}", taskType);
                    asyncTaskService.saveTaskResult(AsyncTaskResult.failed(taskId, "未知任务类型: " + taskType));
                }
            }

        } catch (Exception e) {
            log.error("处理Kafka AI任务失败: key={}, error={}", key, e.getMessage(), e);
            try {
                asyncTaskService.saveTaskResult(AsyncTaskResult.failed(key, "处理失败: " + e.getMessage()));
            } catch (Exception ex) {
                log.error("保存任务结果失败: {}", ex.getMessage());
            }
        }
    }

    /**
     * 处理AI预约推荐任务
     */
    private void handleAppointmentRecommend(String taskId, String patientId, JsonNode json) throws Exception {
        String symptom = json.has("symptom") ? json.get("symptom").asText() : "";
        log.info("处理AI预约推荐任务: taskId={}, symptom={}", taskId, symptom);

        AppointmentRecommendation recommendation = aiAppointmentService.recommendWithSchedules(symptom);
        String resultJson = objectMapper.writeValueAsString(recommendation);
        asyncTaskService.saveTaskResult(AsyncTaskResult.completed(taskId, resultJson));
        log.info("AI预约推荐任务完成: taskId={}", taskId);
    }

    /**
     * 处理AI导诊对话任务
     */
    private void handleAppointmentTriage(String taskId, String patientId, JsonNode json) throws Exception {
        String message = json.has("message") ? json.get("message").asText() : "";
        String historyJson = json.has("historyJson") ? json.get("historyJson").asText() : null;
        log.info("处理AI导诊对话任务: taskId={}, message={}", taskId, message);

        List<ChatMessageDto> history = null;
        if (historyJson != null && !historyJson.isEmpty()) {
            history = objectMapper.readValue(historyJson, new TypeReference<List<ChatMessageDto>>() {});
        }

        TriageChatResponse chatResponse = aiAppointmentService.triageChat(message, history);
        String resultJson = objectMapper.writeValueAsString(chatResponse);
        asyncTaskService.saveTaskResult(AsyncTaskResult.completed(taskId, resultJson));
        log.info("AI导诊对话任务完成: taskId={}", taskId);
    }

    /**
     * 处理AI药品推荐任务
     */
    private void handleMedicineRecommend(String taskId, JsonNode json) throws Exception {
        String symptom = json.has("symptom") ? json.get("symptom").asText() : "";
        log.info("处理AI药品推荐任务: taskId={}, symptom={}", taskId, symptom);

        List<MedicineRecommendation> recommendations = aiMedicineService.recommendBySymptom(symptom);
        String resultJson = objectMapper.writeValueAsString(recommendations);
        asyncTaskService.saveTaskResult(AsyncTaskResult.completed(taskId, resultJson));
        log.info("AI药品推荐任务完成: taskId={}", taskId);
    }

    /**
     * 处理AI药品对话任务
     */
    private void handleMedicineChat(String taskId, JsonNode json) throws Exception {
        String message = json.has("message") ? json.get("message").asText() : "";
        String historyJson = json.has("historyJson") ? json.get("historyJson").asText() : null;
        log.info("处理AI药品对话任务: taskId={}, message={}", taskId, message);

        List<ChatMessageDto> history = null;
        if (historyJson != null && !historyJson.isEmpty()) {
            history = objectMapper.readValue(historyJson, new TypeReference<List<ChatMessageDto>>() {});
        }

        MedicineChatResponse chatResponse = aiMedicineService.medicineChat(message, history);
        String resultJson = objectMapper.writeValueAsString(chatResponse);
        asyncTaskService.saveTaskResult(AsyncTaskResult.completed(taskId, resultJson));
        log.info("AI药品对话任务完成: taskId={}", taskId);
    }

    /**
     * 处理AI费用对话任务
     */
    private void handleBillingChat(String taskId, String patientId, JsonNode json) throws Exception {
        String message = json.has("message") ? json.get("message").asText() : "";
        String startDate = json.has("startDate") ? json.get("startDate").asText() : null;
        String endDate = json.has("endDate") ? json.get("endDate").asText() : null;
        String historyJson = json.has("historyJson") ? json.get("historyJson").asText() : null;
        log.info("处理AI费用对话任务: taskId={}, message={}", taskId, message);

        List<Billing> billings = getBillingsByDateRange(patientId, startDate, endDate);

        List<ChatMessageDto> history = null;
        if (historyJson != null && !historyJson.isEmpty()) {
            history = objectMapper.readValue(historyJson, new TypeReference<List<ChatMessageDto>>() {});
        }

        BillingChatResponse chatResponse = aiBillingService.billingChat(message, history, billings);
        String resultJson = objectMapper.writeValueAsString(chatResponse);
        asyncTaskService.saveTaskResult(AsyncTaskResult.completed(taskId, resultJson));
        log.info("AI费用对话任务完成: taskId={}", taskId);
    }

    /**
     * 处理AI费用解释任务
     */
    private void handleBillingExplain(String taskId, String patientId, JsonNode json) throws Exception {
        String question = json.has("message") ? json.get("message").asText() : "";
        String startDate = json.has("startDate") ? json.get("startDate").asText() : null;
        String endDate = json.has("endDate") ? json.get("endDate").asText() : null;
        log.info("处理AI费用解释任务: taskId={}, question={}", taskId, question);

        List<Billing> billings = getBillingsByDateRange(patientId, startDate, endDate);
        BillingExplanation explanation = aiBillingService.explainBilling(billings, question);

        String resultJson = objectMapper.writeValueAsString(explanation);
        asyncTaskService.saveTaskResult(AsyncTaskResult.completed(taskId, resultJson));
        log.info("AI费用解释任务完成: taskId={}", taskId);
    }

    /**
     * 处理AI报告生成任务（预留）
     */
    private void handleReportGenerate(String taskId, JsonNode json) throws Exception {
        Long recordId = json.has("recordId") ? json.get("recordId").asLong() : null;
        log.info("处理AI报告生成任务: taskId={}, recordId={}", taskId, recordId);

        // TODO: 实现AI报告生成逻辑
        asyncTaskService.saveTaskResult(AsyncTaskResult.failed(taskId, "报告生成功能暂未实现"));
    }

    /**
     * 按日期范围查询费用记录
     */
    private List<Billing> getBillingsByDateRange(String patientId, String startDate, String endDate) {
        QueryWrapper<Billing> wrapper = new QueryWrapper<Billing>()
                .eq("patient_id", patientId);

        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge("create_time", startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le("create_time", endDate + " 23:59:59");
        }

        return billingMapper.selectList(wrapper);
    }
}
