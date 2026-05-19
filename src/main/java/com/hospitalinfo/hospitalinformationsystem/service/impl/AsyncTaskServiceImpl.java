package com.hospitalinfo.hospitalinformationsystem.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalinfo.hospitalinformationsystem.config.RedisStreamConfig;
import com.hospitalinfo.hospitalinformationsystem.dto.AsyncTaskRequest;
import com.hospitalinfo.hospitalinformationsystem.dto.AsyncTaskResult;
import com.hospitalinfo.hospitalinformationsystem.service.IAsyncTaskService;
import com.hospitalinfo.hospitalinformationsystem.service.IKafkaAiProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 异步AI任务服务实现
 * 支持Redis Stream和Kafka两种消息队列
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncTaskServiceImpl implements IAsyncTaskService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final IKafkaAiProducerService kafkaAiProducerService;

    /** 任务结果Key前缀 */
    private static final String TASK_RESULT_PREFIX = "async:task:result:";
    /** 任务结果过期时间：10分钟 */
    private static final long TASK_RESULT_TTL_MINUTES = 10;

    @Override
    public String submitTask(AsyncTaskRequest request) {
        String taskId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        request.setTaskId(taskId);

        try {
            // 1. 先在Redis中创建待处理结果
            AsyncTaskResult pendingResult = AsyncTaskResult.pending(taskId);
            pendingResult.setPatientId(request.getPatientId());
            saveTaskResult(pendingResult);

            // 2. 发送消息到Redis Stream
            Map<String, String> message = new HashMap<>();
            message.put("taskId", taskId);
            message.put("taskType", request.getTaskType());
            message.put("patientId", request.getPatientId() != null ? request.getPatientId() : "");
            message.put("message", request.getMessage() != null ? request.getMessage() : "");
            message.put("startDate", request.getStartDate() != null ? request.getStartDate() : "");
            message.put("endDate", request.getEndDate() != null ? request.getEndDate() : "");
            message.put("symptom", request.getSymptom() != null ? request.getSymptom() : "");
            message.put("reportId", request.getReportId() != null ? request.getReportId().toString() : "");
            message.put("historyJson", request.getHistoryJson() != null ? request.getHistoryJson() : "");

            MapRecord<String, String, String> record = MapRecord.create(
                    RedisStreamConfig.STREAM_AI_TASK, message);
            redisTemplate.opsForStream().add(record);

            log.info("异步任务已提交(Redis Stream): taskId={}, type={}", taskId, request.getTaskType());
            return taskId;
        } catch (Exception e) {
            log.error("提交异步任务失败: {}", e.getMessage(), e);
            // 清理已创建的待处理结果
            redisTemplate.delete(TASK_RESULT_PREFIX + taskId);
            throw new RuntimeException("提交异步任务失败: " + e.getMessage());
        }
    }

    /**
     * 通过Kafka提交异步任务
     */
    @Override
    public String submitTaskViaKafka(AsyncTaskRequest request) {
        String taskId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        request.setTaskId(taskId);

        try {
            // 1. 先在Redis中创建待处理结果
            AsyncTaskResult pendingResult = AsyncTaskResult.pending(taskId);
            pendingResult.setPatientId(request.getPatientId());
            saveTaskResult(pendingResult);

            // 2. 根据任务类型发送到Kafka
            boolean success = false;
            switch (request.getTaskType()) {
                case "APPOINTMENT_RECOMMEND" -> {
                    success = kafkaAiProducerService.submitAiRecommendTask(
                            taskId, request.getPatientId(), request.getSymptom());
                }
                case "APPOINTMENT_TRIAGE" -> {
                    success = kafkaAiProducerService.submitAiTriageTask(
                            taskId, request.getPatientId(), request.getMessage(), request.getHistoryJson());
                }
                case "MEDICINE_RECOMMEND" -> {
                    success = kafkaAiProducerService.submitAiMedicineRecommendTask(
                            taskId, request.getPatientId(), request.getSymptom());
                }
                case "MEDICINE_CHAT" -> {
                    success = kafkaAiProducerService.submitAiMedicineChatTask(
                            taskId, request.getPatientId(), request.getMessage(), request.getHistoryJson());
                }
                case "BILLING_CHAT", "BILLING_EXPLAIN" -> {
                    // 暂不支持，通过Redis Stream
                    return submitTask(request);
                }
                case "REPORT_GENERATE" -> {
                    success = kafkaAiProducerService.submitAiReportTask(
                            taskId, request.getPatientId(), request.getReportId());
                }
                default -> {
                    // 其他类型通过Redis Stream
                    return submitTask(request);
                }
            }

            if (success) {
                log.info("异步任务已提交(Kafka): taskId={}, type={}", taskId, request.getTaskType());
                return taskId;
            } else {
                throw new RuntimeException("Kafka消息发送失败");
            }
        } catch (Exception e) {
            log.error("提交异步任务失败: {}", e.getMessage(), e);
            // 清理已创建的待处理结果
            redisTemplate.delete(TASK_RESULT_PREFIX + taskId);
            throw new RuntimeException("提交异步任务失败: " + e.getMessage());
        }
    }

    @Override
    public AsyncTaskResult getTaskResult(String taskId) {
        String key = TASK_RESULT_PREFIX + taskId;
        Object obj = redisTemplate.opsForValue().get(key);
        if (obj == null) {
            // 任务不存在或已过期
            AsyncTaskResult notFound = new AsyncTaskResult();
            notFound.setTaskId(taskId);
            notFound.setStatus("NOT_FOUND");
            notFound.setErrorMsg("任务不存在或已过期");
            return notFound;
        }
        if (obj instanceof AsyncTaskResult) {
            return (AsyncTaskResult) obj;
        }
        // 尝试JSON转换
        try {
            return objectMapper.convertValue(obj, AsyncTaskResult.class);
        } catch (Exception e) {
            log.error("解析任务结果失败: {}", e.getMessage());
            AsyncTaskResult error = new AsyncTaskResult();
            error.setTaskId(taskId);
            error.setStatus("FAILED");
            error.setErrorMsg("解析任务结果失败");
            return error;
        }
    }

    @Override
    public void saveTaskResult(AsyncTaskResult result) {
        String key = TASK_RESULT_PREFIX + result.getTaskId();
        redisTemplate.opsForValue().set(key, result, TASK_RESULT_TTL_MINUTES, TimeUnit.MINUTES);
    }
}
