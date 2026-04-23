package com.hospitalinfo.hospitalinformationsystem.dto;

import lombok.Data;

/**
 * 异步AI任务请求
 */
@Data
public class AsyncTaskRequest {

    /** 任务ID */
    private String taskId;

    /** 任务类型：BILLING_CHAT / BILLING_EXPLAIN / MEDICINE_CHAT / MEDICINE_RECOMMEND / REPORT_GENERATE */
    private String taskType;

    /** 患者ID */
    private String patientId;

    /** 用户消息 */
    private String message;

    /** 日期范围-开始 */
    private String startDate;

    /** 日期范围-结束 */
    private String endDate;

    /** 症状（药品推荐用） */
    private String symptom;

    /** 报告ID（报告生成用） */
    private Long reportId;

    /** 对话历史JSON */
    private String historyJson;
}
