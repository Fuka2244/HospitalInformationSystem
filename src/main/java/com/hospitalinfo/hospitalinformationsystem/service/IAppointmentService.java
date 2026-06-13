package com.hospitalinfo.hospitalinformationsystem.service;

import com.hospitalinfo.hospitalinformationsystem.dto.*;

import java.util.List;

public interface IAppointmentService {
    Result createAppointment(AppointmentCreateDto dto, String patientId);

    Result createAppointmentWithRedisAndOptimisticLock(AppointmentCreateDto dto, String patientId);

    Result listAppointments(String patientId, AppointmentQueryDto queryDto);

    Result getAppointmentDetail(Long appointmentId);

    Result cancelAppointment(Long appointmentId, String cancelReason, String patientId);

    Result cancelAppointmentWithRedisAndOptimisticLock(Long appointmentId, String cancelReason, String patientId);

    Result rescheduleAppointment(Long appointmentId, AppointmentCreateDto dto, String patientId);

    Result aiRecommendAppointment(String symptom);

    Result aiRecommendWithSchedules(String symptom);

    Result aiTriageChat(String message, List<ChatMessageDto> history);

    Result getAvailableSchedules(Long departmentId, Long doctorId, String date);

    Result syncStockToRedis();

    Result registration(Long appointmentId, String patientId);

    Result frontDeskRegistration(FrontDeskRegistrationDto dto);

    Result getLocation(Long appointmentId, String patientId);
}
