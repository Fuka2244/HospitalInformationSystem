package com.hospitalinfo.hospitalinformationsystem.dto;

import lombok.Data;

/**
 * 异步AI任务结果
 */
@Data
public class AsyncTaskResult {

    /** 任务ID */
    private String taskId;

    /** 任务状态：PENDING / PROCESSING / COMPLETED / FAILED */
    private String status;

    /** 结果数据（JSON格式） */
    private String resultJson;

    /** 错误信息 */
    private String errorMsg;

    /** 创建时间 */
    private long createTime;

    /** 完成时间 */
    private long finishTime;

    public static AsyncTaskResult pending(String taskId) {
        AsyncTaskResult r = new AsyncTaskResult();
        r.setTaskId(taskId);
        r.setStatus("PENDING");
        r.setCreateTime(System.currentTimeMillis());
        return r;
    }

    public static AsyncTaskResult processing(String taskId) {
        AsyncTaskResult r = new AsyncTaskResult();
        r.setTaskId(taskId);
        r.setStatus("PROCESSING");
        r.setCreateTime(System.currentTimeMillis());
        return r;
    }

    public static AsyncTaskResult completed(String taskId, String resultJson) {
        AsyncTaskResult r = new AsyncTaskResult();
        r.setTaskId(taskId);
        r.setStatus("COMPLETED");
        r.setResultJson(resultJson);
        r.setCreateTime(System.currentTimeMillis());
        r.setFinishTime(System.currentTimeMillis());
        return r;
    }

    public static AsyncTaskResult failed(String taskId, String errorMsg) {
        AsyncTaskResult r = new AsyncTaskResult();
        r.setTaskId(taskId);
        r.setStatus("FAILED");
        r.setErrorMsg(errorMsg);
        r.setCreateTime(System.currentTimeMillis());
        r.setFinishTime(System.currentTimeMillis());
        return r;
    }
}
