package com.hospitalinfo.hospitalinformationsystem.service;

import com.hospitalinfo.hospitalinformationsystem.dto.*;

/**
 * 预约系统服务接口
 */
public interface IAppointmentService {

    /** 创建预约 */
    Result createAppointment(AppointmentCreateDto dto, String patientId);

    /** 分页查询预约列表 */
    Result listAppointments(String patientId, AppointmentQueryDto queryDto);

    /** 获取预约详情 */
    Result getAppointmentDetail(Long appointmentId);

    /** 取消预约 */
    Result cancelAppointment(Long appointmentId, String cancelReason, String patientId);

    /** 改期 */
    Result rescheduleAppointment(Long appointmentId, AppointmentCreateDto dto, String patientId);

    /** AI智能预约推荐 */
    Result aiRecommendAppointment(String symptom);

    /** AI智能预约推荐并查询可用排班 */
    Result aiRecommendWithSchedules(String symptom);

    /** 获取可用排班 */
    Result getAvailableSchedules(Long departmentId, Long doctorId, String date);
}
