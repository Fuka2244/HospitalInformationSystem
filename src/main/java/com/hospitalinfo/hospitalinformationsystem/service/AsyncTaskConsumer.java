package com.hospitalinfo.hospitalinformationsystem.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalinfo.hospitalinformationsystem.ai.AiBillingService;
import com.hospitalinfo.hospitalinformationsystem.ai.AiMedicineService;
import com.hospitalinfo.hospitalinformationsystem.dto.*;
import com.hospitalinfo.hospitalinformationsystem.entity.Billing;
import com.hospitalinfo.hospitalinformationsystem.mapper.BillingMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Redis Stream 消息消费者
 * 异步处理AI任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncTaskConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    private final IAsyncTaskService asyncTaskService;
    private final AiBillingService aiBillingService;
    private final AiMedicineService aiMedicineService;
    private final BillingMapper billingMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        Map<String, String> value = message.getValue();
        String taskId = value.get("taskId");
        String taskType = value.get("taskType");

        log.info("收到异步任务: taskId={}, type={}", taskId, taskType);

        // 更新状态为处理中
        asyncTaskService.saveTaskResult(AsyncTaskResult.processing(taskId));

        try {
            switch (taskType) {
                case "BILLING_CHAT" -> handleBillingChat(value);
                case "BILLING_EXPLAIN" -> handleBillingExplain(value);
                case "MEDICINE_CHAT" -> handleMedicineChat(value);
                case "MEDICINE_RECOMMEND" -> handleMedicineRecommend(value);
                default -> {
                    log.warn("未知任务类型: {}", taskType);
                    asyncTaskService.saveTaskResult(AsyncTaskResult.failed(taskId, "未知任务类型: " + taskType));
                }
            }
        } catch (Exception e) {
            log.error("处理异步任务失败: taskId={}, error={}", taskId, e.getMessage(), e);
            asyncTaskService.saveTaskResult(AsyncTaskResult.failed(taskId, "处理失败: " + e.getMessage()));
        }
    }

    /**
     * 处理AI费用对话任务
     */
    private void handleBillingChat(java.util.Map<String, String> value) throws Exception {
        String taskId = value.get("taskId");
        String patientId = value.get("patientId");
        String message = value.get("message");
        String startDate = value.get("startDate");
        String endDate = value.get("endDate");
        String historyJson = value.get("historyJson");

        // 查询费用数据
        List<Billing> billings = getBillingsByDateRange(patientId, startDate, endDate);

        // 解析对话历史
        List<ChatMessageDto> history = null;
        if (historyJson != null && !historyJson.isEmpty()) {
            history = objectMapper.readValue(historyJson, new TypeReference<List<ChatMessageDto>>() {});
        }

        // 调用AI服务
        BillingChatResponse chatResponse = aiBillingService.billingChat(message, history, billings);

        // 保存结果
        String resultJson = objectMapper.writeValueAsString(chatResponse);
        asyncTaskService.saveTaskResult(AsyncTaskResult.completed(taskId, resultJson));
        log.info("AI费用对话任务完成: taskId={}", taskId);
    }

    /**
     * 处理AI费用解释任务
     */
    private void handleBillingExplain(java.util.Map<String, String> value) throws Exception {
        String taskId = value.get("taskId");
        String patientId = value.get("patientId");
        String question = value.get("message");
        String startDate = value.get("startDate");
        String endDate = value.get("endDate");

        List<Billing> billings = getBillingsByDateRange(patientId, startDate, endDate);
        BillingExplanation explanation = aiBillingService.explainBilling(billings, question);

        String resultJson = objectMapper.writeValueAsString(explanation);
        asyncTaskService.saveTaskResult(AsyncTaskResult.completed(taskId, resultJson));
        log.info("AI费用解释任务完成: taskId={}", taskId);
    }

    /**
     * 处理AI药品对话任务
     */
    private void handleMedicineChat(java.util.Map<String, String> value) throws Exception {
        String taskId = value.get("taskId");
        String message = value.get("message");
        String historyJson = value.get("historyJson");

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
     * 处理AI药品推荐任务
     */
    private void handleMedicineRecommend(java.util.Map<String, String> value) throws Exception {
        String taskId = value.get("taskId");
        String symptom = value.get("symptom");

        List<MedicineRecommendation> recommendations = aiMedicineService.recommendBySymptom(symptom);

        String resultJson = objectMapper.writeValueAsString(recommendations);
        asyncTaskService.saveTaskResult(AsyncTaskResult.completed(taskId, resultJson));
        log.info("AI药品推荐任务完成: taskId={}", taskId);
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
