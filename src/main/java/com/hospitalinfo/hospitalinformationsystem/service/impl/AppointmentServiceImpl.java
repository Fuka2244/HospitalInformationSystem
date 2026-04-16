package com.hospitalinfo.hospitalinformationsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospitalinfo.hospitalinformationsystem.ai.AiAppointmentService;
import com.hospitalinfo.hospitalinformationsystem.dto.*;
import com.hospitalinfo.hospitalinformationsystem.entity.*;
import com.hospitalinfo.hospitalinformationsystem.mapper.*;
import com.hospitalinfo.hospitalinformationsystem.service.IAppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements IAppointmentService {

    private final AppointmentMapper appointmentMapper;
    private final DoctorScheduleMapper doctorScheduleMapper;
    private final DoctorMapper doctorMapper;
    private final DepartmentMapper departmentMapper;
    private final PatientMapper patientMapper;
    private final AiAppointmentService aiAppointmentService;

    @Override
    @Transactional
    public Result createAppointment(AppointmentCreateDto dto, String patientId) {
        // 校验排班是否存在且有余号
        if (dto.getDoctorId() != null) {
            DoctorSchedule schedule = doctorScheduleMapper.selectOne(
                    new QueryWrapper<DoctorSchedule>()
                            .eq("doctor_id", dto.getDoctorId())
                            .eq("schedule_date", dto.getAppointmentDate())
                            .eq("time_slot", dto.getTimeSlot()));

            if (schedule == null) {
                return Result.fail("该时段无排班，请选择其他时间");
            }
            if (schedule.getBookedCount() >= schedule.getMaxPatients()) {
                return Result.fail("该时段已约满，请选择其他时间");
            }

            // 更新已预约数
            schedule.setBookedCount(schedule.getBookedCount() + 1);
            doctorScheduleMapper.updateById(schedule);
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

        // 释放排班号源
        if (appointment.getDoctorId() != null) {
            DoctorSchedule schedule = doctorScheduleMapper.selectOne(
                    new QueryWrapper<DoctorSchedule>()
                            .eq("doctor_id", appointment.getDoctorId())
                            .eq("schedule_date", appointment.getAppointmentDate())
                            .eq("time_slot", appointment.getTimeSlot()));
            if (schedule != null && schedule.getBookedCount() > 0) {
                schedule.setBookedCount(schedule.getBookedCount() - 1);
                doctorScheduleMapper.updateById(schedule);
            }
        }

        return Result.ok();
    }

    @Override
    @Transactional
    public Result rescheduleAppointment(Long appointmentId, AppointmentCreateDto dto, String patientId) {
        // 先取消原预约
        Result cancelResult = cancelAppointment(appointmentId, "改期", patientId);
        if (!cancelResult.getSuccess()) {
            return cancelResult;
        }
        // 创建新预约
        return createAppointment(dto, patientId);
    }

    @Override
    public Result aiRecommendAppointment(String symptom) {
        AppointmentRecommendation recommendation = aiAppointmentService.recommendBySymptom(symptom);
        return Result.ok(recommendation);
    }

    @Override
    public Result aiRecommendWithSchedules(String symptom) {
        // 获取AI推荐并查询可用排班
        AppointmentRecommendation recommendation = aiAppointmentService.recommendWithSchedules(symptom);

        // 如果没有可用的医生ID，返回推荐结果
        if (recommendation.getDoctorId() == null || recommendation.getRecommendedDate() == null) {
            return Result.ok(recommendation);
        }

        // 查询该医生在该日期的所有可用排班
        LocalDate appointmentDate = LocalDate.parse(recommendation.getRecommendedDate());
        QueryWrapper<DoctorSchedule> wrapper = new QueryWrapper<DoctorSchedule>()
                .eq("doctor_id", recommendation.getDoctorId())
                .eq("schedule_date", appointmentDate)
                .eq("status", 1)
                .orderByAsc("time_slot");

        List<DoctorSchedule> schedules = doctorScheduleMapper.selectList(wrapper);

        // 过滤出可用的排班（未约满）
        List<DoctorSchedule> availableSchedules = schedules.stream()
                .filter(s -> s.getBookedCount() < s.getMaxPatients())
                .toList();

        // 填充医生名称和科室名称
        for (DoctorSchedule schedule : availableSchedules) {
            Doctor doctor = doctorMapper.selectById(schedule.getDoctorId());
            if (doctor != null) {
                schedule.setDoctorName(doctor.getName());
                Department dept = departmentMapper.selectById(doctor.getDepartmentId());
                if (dept != null) {
                    schedule.setDepartmentName(dept.getName());
                }
            }
        }

        // 将可用排班列表放入推荐结果中
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("recommendation", recommendation);
        result.put("availableSchedules", availableSchedules);

        return Result.ok(result);
    }

    @Override
    public Result getAvailableSchedules(Long departmentId, Long doctorId, String date) {
        QueryWrapper<DoctorSchedule> wrapper = new QueryWrapper<>();
        if (departmentId != null) {
            // 先查该科室下的医生
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
            // 默认查未来7天
            wrapper.ge("schedule_date", LocalDate.now());
            wrapper.le("schedule_date", LocalDate.now().plusDays(7));
        }
        wrapper.eq("status", 1);
        wrapper.orderByAsc("schedule_date", "time_slot");

        List<DoctorSchedule> schedules = doctorScheduleMapper.selectList(wrapper);
        // 填充医生名称和科室名称
        for (DoctorSchedule schedule : schedules) {
            Doctor doctor = doctorMapper.selectById(schedule.getDoctorId());
            if (doctor != null) {
                schedule.setDoctorName(doctor.getName());
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
