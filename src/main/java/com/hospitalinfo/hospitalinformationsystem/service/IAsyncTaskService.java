package com.hospitalinfo.hospitalinformationsystem.service;

import com.hospitalinfo.hospitalinformationsystem.dto.AsyncTaskRequest;
import com.hospitalinfo.hospitalinformationsystem.dto.AsyncTaskResult;

/**
 * 异步AI任务服务接口
 * 支持Redis Stream和Kafka两种消息队列
 */
public interface IAsyncTaskService {

    /**
     * 提交异步任务（默认使用Redis Stream）
     * @param request 任务请求
     * @return 任务ID
     */
    String submitTask(AsyncTaskRequest request);

    /**
     * 提交异步任务到Kafka
     * @param request 任务请求
     * @return 任务ID
     */
    String submitTaskViaKafka(AsyncTaskRequest request);

    /**
     * 获取任务结果
     * @param taskId 任务ID
     * @return 任务结果（可能还在处理中）
     */
    AsyncTaskResult getTaskResult(String taskId);

    /**
     * 保存任务结果到Redis
     * @param result 任务结果
     */
    void saveTaskResult(AsyncTaskResult result);
}
