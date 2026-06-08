package com.hospitalinfo.hospitalinformationsystem.service;

/**
 * Kafka AI消息生产者服务接口
 * 用于发送AI任务到Kafka消息队列
 */
public interface IKafkaAiProducerService {

    /**
     * 提交AI预约推荐任务
     * @param taskId 任务ID
     * @param patientId 患者ID
     * @param symptom 症状描述
     * @return 是否提交成功
     */
    boolean submitAiRecommendTask(String taskId, String patientId, String symptom);

    /**
     * 提交AI多轮导诊任务
     * @param taskId 任务ID
     * @param patientId 患者ID
     * @param message 用户消息
     * @param historyJson 历史对话JSON
     * @return 是否提交成功
     */
    boolean submitAiTriageTask(String taskId, String patientId, String message, String historyJson);

    /**
     * 提交AI药品推荐任务
     * @param taskId 任务ID
     * @param patientId 患者ID
     * @param symptom 症状描述
     * @return 是否提交成功
     */
    boolean submitAiMedicineRecommendTask(String taskId, String patientId, String symptom);

    /**
     * 提交AI药品对话任务
     * @param taskId 任务ID
     * @param patientId 患者ID
     * @param message 用户消息
     * @param historyJson 历史对话JSON
     * @return 是否提交成功
     */
    boolean submitAiMedicineChatTask(String taskId, String patientId, String message, String historyJson);

    /**
     * 提交AI报告生成任务
     * @param taskId 任务ID
     * @param patientId 患者ID
     * @param recordId 病历ID
     * @return 是否提交成功
     */
    boolean submitAiReportTask(String taskId, String patientId, Long recordId);
}
