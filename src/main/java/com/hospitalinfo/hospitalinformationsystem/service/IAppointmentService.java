package com.hospitalinfo.hospitalinformationsystem.service;

import com.hospitalinfo.hospitalinformationsystem.dto.*;

import java.util.List;

/**
 * 预约系统服务接口
 */
public interface IAppointmentService {

    /** 创建预约（分布式锁版本） */
    Result createAppointment(AppointmentCreateDto dto, String patientId);

    /**
     * 创建预约（Redis预扣减 + MySQL乐观锁版本）
     * 适用于高并发场景，Redis作为第一道防线，MySQL作为最终一致性保障
     */
    Result createAppointmentWithRedisAndOptimisticLock(AppointmentCreateDto dto, String patientId);

    /** 分页查询预约列表 */
    Result listAppointments(String patientId, AppointmentQueryDto queryDto);

    /** 获取预约详情 */
    Result getAppointmentDetail(Long appointmentId);

    /** 取消预约 */
    Result cancelAppointment(Long appointmentId, String cancelReason, String patientId);

    /**
     * 取消预约（Redis预扣减 + MySQL乐观锁版本）
     */
    Result cancelAppointmentWithRedisAndOptimisticLock(Long appointmentId, String cancelReason, String patientId);

    /** 改期 */
    Result rescheduleAppointment(Long appointmentId, AppointmentCreateDto dto, String patientId);

    /** AI智能预约推荐 */
    Result aiRecommendAppointment(String symptom);

    /** AI智能预约推荐并查询可用排班 */
    Result aiRecommendWithSchedules(String symptom);

    /** AI多轮对话式智能导诊 */
    Result aiTriageChat(String message, List<ChatMessageDto> history);

    /** 获取可用排班 */
    Result getAvailableSchedules(Long departmentId, Long doctorId, String date);

    /**
     * 同步库存到Redis（定时任务调用）
     * 将数据库中的号源库存同步到Redis缓存
     */
    Result syncStockToRedis();
}
