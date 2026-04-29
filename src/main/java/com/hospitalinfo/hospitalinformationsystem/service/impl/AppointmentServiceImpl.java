package com.hospitalinfo.hospitalinformationsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospitalinfo.hospitalinformationsystem.ai.AiAppointmentService;
import com.hospitalinfo.hospitalinformationsystem.config.CacheConfig;
import com.hospitalinfo.hospitalinformationsystem.dto.*;
import com.hospitalinfo.hospitalinformationsystem.entity.*;
import com.hospitalinfo.hospitalinformationsystem.mapper.*;
import com.hospitalinfo.hospitalinformationsystem.service.IAppointmentService;
import com.hospitalinfo.hospitalinformationsystem.utils.RedisDistributedLock;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements IAppointmentService {

    private final AppointmentMapper appointmentMapper;
    private final DoctorScheduleMapper doctorScheduleMapper;
    private final DoctorMapper doctorMapper;
    private final DepartmentMapper departmentMapper;
    private final PatientMapper patientMapper;
    private final AiAppointmentService aiAppointmentService;
    private final RedisDistributedLock distributedLock;

    /** 分布式锁Key前缀 */
    private static final String APPOINTMENT_LOCK_PREFIX = "lock:appointment:";

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_SCHEDULE, allEntries = true)
    public Result createAppointment(AppointmentCreateDto dto, String patientId) {
        if (dto.getDoctorId() != null) {
            // 分布式锁：按排班维度加锁，同一排班同时只能有一个预约操作
            String lockKey = APPOINTMENT_LOCK_PREFIX + dto.getDoctorId() + ":"
                    + dto.getAppointmentDate() + ":" + dto.getTimeSlot();
            String lockValue = distributedLock.tryLock(lockKey);

            if (lockValue == null) {
                return Result.fail("当前预约人数较多，请稍后再试");
            }

            try {
                // 防重复预约：同一患者不能重复预约同一医生同一时段
                Long existCount = appointmentMapper.selectCount(
                        new QueryWrapper<Appointment>()
                                .eq("patient_id", patientId)
                                .eq("doctor_id", dto.getDoctorId())
                                .eq("appointment_date", dto.getAppointmentDate())
                                .eq("time_slot", dto.getTimeSlot())
                                .in("status", 0, 1));
                if (existCount > 0) {
                    return Result.fail("您已预约该时段，请勿重复预约");
                }

                // 原子操作递增已预约数，根据返回值判断是否约满
                int updated = doctorScheduleMapper.incrementBookedCount(
                        dto.getDoctorId(), dto.getAppointmentDate(), dto.getTimeSlot());
                if (updated == 0) {
                    return Result.fail("该时段已约满，请选择其他时间");
                }
            } finally {
                distributedLock.unlock(lockKey, lockValue);
            }
        }

        // 创建预约
        Appointment appointment = new Appointment();
        appointment.setPatientId(patientId);
        appointment.setDoctorId(dto.getDoctorId());
        appointment.setDepartmentId(dto.getDepartmentId());
        appointment.setAppointmentType(dto.getAppointmentType());
        appointment.setExaminationType(dto.getExaminationType());
        appointment.setAppointmentDate(dto.getAppointmentDate());
        appointment.setTimeSlot(dto.getTimeSlot());
        appointment.setStatus(0);
        appointment.setAiRecommended(0);

        appointmentMapper.insert(appointment);
        return Result.ok(appointment);
    }

    @Override
    public Result listAppointments(String patientId, AppointmentQueryDto queryDto) {
        Page<Appointment> pageParam = new Page<>(queryDto.getPage(), queryDto.getSize());
        QueryWrapper<Appointment> wrapper = new QueryWrapper<Appointment>()
                .eq("patient_id", patientId);

        if (queryDto.getStatus() != null) {
            wrapper.eq("status", queryDto.getStatus());
        }
        if (queryDto.getDepartmentId() != null) {
            wrapper.eq("department_id", queryDto.getDepartmentId());
        }
        if (queryDto.getDoctorId() != null) {
            wrapper.eq("doctor_id", queryDto.getDoctorId());
        }
        if (queryDto.getStartDate() != null && !queryDto.getStartDate().isEmpty()) {
            wrapper.ge("appointment_date", queryDto.getStartDate());
        }
        if (queryDto.getEndDate() != null && !queryDto.getEndDate().isEmpty()) {
            wrapper.le("appointment_date", queryDto.getEndDate());
        }
        wrapper.orderByDesc("appointment_date");

        Page<Appointment> result = appointmentMapper.selectPage(pageParam, wrapper);
        fillAppointmentNames(result.getRecords());

        return Result.ok(result.getRecords(), result.getTotal());
    }

    @Override
    public Result getAppointmentDetail(Long appointmentId) {
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            return Result.fail("预约不存在");
        }
        fillAppointmentNames(List.of(appointment));
        return Result.ok(appointment);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_SCHEDULE, allEntries = true)
    public Result cancelAppointment(Long appointmentId, String cancelReason, String patientId) {
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            return Result.fail("预约不存在");
        }
        if (!appointment.getPatientId().equals(patientId)) {
            return Result.fail("无权操作此预约");
        }
        if (appointment.getStatus() != 0) {
            return Result.fail("只能取消已预约状态的预约");
        }

        appointment.setStatus(2);
        appointment.setCancelReason(cancelReason);
        appointmentMapper.updateById(appointment);

        // 释放排班号源：加分布式锁 + 原子操作
        if (appointment.getDoctorId() != null) {
            String lockKey = APPOINTMENT_LOCK_PREFIX + appointment.getDoctorId() + ":"
                    + appointment.getAppointmentDate() + ":" + appointment.getTimeSlot();
            String lockValue = distributedLock.tryLock(lockKey);
            if (lockValue != null) {
                try {
                    doctorScheduleMapper.decrementBookedCount(
                            appointment.getDoctorId(), appointment.getAppointmentDate(), appointment.getTimeSlot());
                } finally {
                    distributedLock.unlock(lockKey, lockValue);
                }
            } else {
                doctorScheduleMapper.decrementBookedCount(
                        appointment.getDoctorId(), appointment.getAppointmentDate(), appointment.getTimeSlot());
            }
        }

        return Result.ok();
    }

    @Override
    @Transactional
    public Result rescheduleAppointment(Long appointmentId, AppointmentCreateDto dto, String patientId) {
        Result cancelResult = cancelAppointment(appointmentId, "改期", patientId);
        if (!cancelResult.getSuccess()) {
            return cancelResult;
        }
        return createAppointment(dto, patientId);
    }

    @Override
    public Result aiRecommendAppointment(String symptom) {
        AppointmentRecommendation recommendation = aiAppointmentService.recommendBySymptom(symptom);
        return Result.ok(recommendation);
    }

    @Override
    public Result aiRecommendWithSchedules(String symptom) {
        AppointmentRecommendation recommendation = aiAppointmentService.recommendWithSchedules(symptom);
        return Result.ok(recommendation);
    }

    @Override
    public Result aiTriageChat(String message, List<ChatMessageDto> history) {
        if (message == null || message.trim().isEmpty()) {
            return Result.fail("请输入您的消息");
        }
        TriageChatResponse chatResponse = aiAppointmentService.triageChat(message, history);
        return Result.ok(chatResponse);
    }

    @Override
    public Result getAvailableSchedules(Long departmentId, Long doctorId, String date) {
        // 优先走缓存
        if (departmentId != null && doctorId == null && (date == null || date.isEmpty())) {
            // 查科室下所有排班（走缓存）
            List<DoctorSchedule> schedules = aiAppointmentService.getDepartmentSchedulesFromCache(departmentId);
            List<Doctor> doctors = aiAppointmentService.getDoctorsByDepartmentFromCache(departmentId);
            Department dept = departmentMapper.selectById(departmentId);
            for (DoctorSchedule schedule : schedules) {
                doctors.stream()
                        .filter(d -> d.getId().equals(schedule.getDoctorId()))
                        .findFirst()
                        .ifPresent(d -> {
                            schedule.setDoctorName(d.getName());
                            schedule.setDepartmentId(departmentId);
                            schedule.setDepartmentName(dept != null ? dept.getName() : "");
                        });
            }
            return Result.ok(schedules);
        }

        // 其他情况直接查DB
        QueryWrapper<DoctorSchedule> wrapper = new QueryWrapper<>();
        if (departmentId != null) {
            List<Doctor> doctors = doctorMapper.selectList(
                    new QueryWrapper<Doctor>().eq("department_id", departmentId).eq("status", 1));
            List<Long> doctorIds = doctors.stream().map(Doctor::getId).toList();
            if (doctorIds.isEmpty()) {
                return Result.ok(List.of());
            }
            wrapper.in("doctor_id", doctorIds);
        }
        if (doctorId != null) {
            wrapper.eq("doctor_id", doctorId);
        }
        if (date != null && !date.isEmpty()) {
            wrapper.eq("schedule_date", LocalDate.parse(date));
        } else {
            wrapper.ge("schedule_date", LocalDate.now());
            wrapper.le("schedule_date", LocalDate.now().plusDays(7));
        }
        wrapper.eq("status", 1);
        wrapper.orderByAsc("schedule_date", "time_slot");

        List<DoctorSchedule> schedules = doctorScheduleMapper.selectList(wrapper);
        for (DoctorSchedule schedule : schedules) {
            Doctor doctor = doctorMapper.selectById(schedule.getDoctorId());
            if (doctor != null) {
                schedule.setDoctorName(doctor.getName());
                schedule.setDepartmentId(doctor.getDepartmentId());
                Department dept = departmentMapper.selectById(doctor.getDepartmentId());
                if (dept != null) {
                    schedule.setDepartmentName(dept.getName());
                }
            }
        }

        return Result.ok(schedules);
    }

    private void fillAppointmentNames(List<Appointment> appointments) {
        for (Appointment apt : appointments) {
            Patient patient = patientMapper.selectById(apt.getPatientId());
            if (patient != null) apt.setPatientName(patient.getName());
            if (apt.getDoctorId() != null) {
                Doctor doctor = doctorMapper.selectById(apt.getDoctorId());
                if (doctor != null) apt.setDoctorName(doctor.getName());
            }
            Department dept = departmentMapper.selectById(apt.getDepartmentId());
            if (dept != null) apt.setDepartmentName(dept.getName());
        }
    }
}
