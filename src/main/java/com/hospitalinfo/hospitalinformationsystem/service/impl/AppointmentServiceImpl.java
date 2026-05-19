package com.hospitalinfo.hospitalinformationsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospitalinfo.hospitalinformationsystem.ai.AiAppointmentService;
import com.hospitalinfo.hospitalinformationsystem.config.CacheConfig;
import com.hospitalinfo.hospitalinformationsystem.dto.*;
import com.hospitalinfo.hospitalinformationsystem.entity.*;
import com.hospitalinfo.hospitalinformationsystem.mapper.*;
import com.hospitalinfo.hospitalinformationsystem.service.IAppointmentService;
import com.hospitalinfo.hospitalinformationsystem.service.IScheduleStockService;
import com.hospitalinfo.hospitalinformationsystem.utils.RedisDistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
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
    private final IScheduleStockService scheduleStockService;

    /** 分布式锁Key前缀 */
    private static final String APPOINTMENT_LOCK_PREFIX = "lock:appointment:";

    @Override
    @CacheEvict(value = CacheConfig.CACHE_SCHEDULE, allEntries = true)
    public Result createAppointment(AppointmentCreateDto dto, String patientId) {
        if (dto.getDoctorId() == null) {
            return Result.fail("请选择医生和就诊时段");
        }

        // 分布式锁：按排班维度加锁
        String lockKey = buildLockKey(dto.getDoctorId(), dto.getAppointmentDate().toString(), dto.getTimeSlot());
        String lockValue = distributedLock.tryLock(lockKey);

        if (lockValue == null) {
            return Result.fail("当前预约人数较多，请稍后再试");
        }

        try {
            return doCreateAppointmentWithLock(dto, patientId);
        } finally {
            distributedLock.unlock(lockKey, lockValue);
        }
    }

    /**
     * Redis预扣减 + MySQL乐观锁创建预约
     * 适用于高并发热门号源场景
     */
    @Override
    @CacheEvict(value = CacheConfig.CACHE_SCHEDULE, allEntries = true)
    public Result createAppointmentWithRedisAndOptimisticLock(AppointmentCreateDto dto, String patientId) {
        if (dto.getDoctorId() == null) {
            return Result.fail("请选择医生和就诊时段");
        }

        String scheduleDateStr = dto.getAppointmentDate().toString();
        String scheduleKey = buildScheduleKey(dto.getDoctorId(), scheduleDateStr, dto.getTimeSlot());

        // 防重复预约检查
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

        // 第一步：Redis预扣减库存（快速过滤大部分无效请求）
        if (!scheduleStockService.isStockInitialized(scheduleKey)) {
            // Redis未初始化，先查数据库
            DoctorSchedule schedule = doctorScheduleMapper.selectScheduleWithVersion(
                    dto.getDoctorId(), dto.getAppointmentDate(), dto.getTimeSlot());
            if (schedule == null) {
                return Result.fail("该排班不存在");
            }
            // 初始化Redis库存
            int availableStock = schedule.getMaxPatients() - schedule.getBookedCount();
            scheduleStockService.initStockBatch(scheduleKey, availableStock);
        }

        boolean preDecrementSuccess = scheduleStockService.preDecrementStock(scheduleKey);
        if (!preDecrementSuccess) {
            return Result.fail("该时段已约满，请选择其他时间");
        }

        // 第二步：MySQL乐观锁确保最终一致性
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                // 查询当前版本
                DoctorSchedule schedule = doctorScheduleMapper.selectScheduleWithVersion(
                        dto.getDoctorId(), dto.getAppointmentDate(), dto.getTimeSlot());
                if (schedule == null) {
                    scheduleStockService.incrementStock(scheduleKey);
                    return Result.fail("该排班不存在");
                }

                if (schedule.getBookedCount() >= schedule.getMaxPatients()) {
                    scheduleStockService.incrementStock(scheduleKey);
                    return Result.fail("该时段已约满");
                }

                // 乐观锁更新
                int updated = doctorScheduleMapper.incrementBookedCountWithVersion(
                        dto.getDoctorId(), dto.getAppointmentDate(), dto.getTimeSlot(),
                        schedule.getVersion());

                if (updated > 0) {
                    // 乐观锁更新成功，创建预约记录
                    return doCreateAppointment(dto, patientId);
                }

                // 版本冲突，重试
                log.info("乐观锁版本冲突，重试第{}次: scheduleKey={}", i + 1, scheduleKey);

            } catch (Exception e) {
                log.error("预约创建异常: scheduleKey={}, error={}", scheduleKey, e.getMessage());
                scheduleStockService.incrementStock(scheduleKey);
                return Result.fail("预约创建失败，请稍后再试");
            }
        }

        // 重试次数用完
        scheduleStockService.incrementStock(scheduleKey);
        return Result.fail("预约创建失败，请稍后再试");
    }

    /**
     * 在分布式锁保护下执行预约创建
     */
    private Result doCreateAppointmentWithLock(AppointmentCreateDto dto, String patientId) {
        // 防重复预约检查
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

        // 原子操作递增已预约数
        int updated = doctorScheduleMapper.incrementBookedCount(
                dto.getDoctorId(), dto.getAppointmentDate(), dto.getTimeSlot());
        if (updated == 0) {
            return Result.fail("该时段已约满，请选择其他时间");
        }

        return doCreateAppointment(dto, patientId);
    }

    /**
     * 事务性创建预约记录
     */
    @Transactional
    public Result doCreateAppointment(AppointmentCreateDto dto, String patientId) {
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
        log.info("预约创建成功: patientId={}, doctorId={}, date={}, timeSlot={}",
                patientId, dto.getDoctorId(), dto.getAppointmentDate(), dto.getTimeSlot());
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

        // 释放排班号源
        if (appointment.getDoctorId() != null) {
            String lockKey = buildLockKey(appointment.getDoctorId(),
                    appointment.getAppointmentDate().toString(), appointment.getTimeSlot());
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

    /**
     * Redis预扣减 + MySQL乐观锁取消预约
     */
    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_SCHEDULE, allEntries = true)
    public Result cancelAppointmentWithRedisAndOptimisticLock(Long appointmentId, String cancelReason, String patientId) {
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

        // 更新预约状态
        appointment.setStatus(2);
        appointment.setCancelReason(cancelReason);
        appointmentMapper.updateById(appointment);

        // 释放库存
        if (appointment.getDoctorId() != null) {
            String scheduleDateStr = appointment.getAppointmentDate().toString();
            String scheduleKey = buildScheduleKey(appointment.getDoctorId(),
                    scheduleDateStr, appointment.getTimeSlot());

            // 回补Redis库存
            scheduleStockService.incrementStock(scheduleKey);

            // MySQL乐观锁更新
            DoctorSchedule schedule = doctorScheduleMapper.selectScheduleWithVersion(
                    appointment.getDoctorId(), appointment.getAppointmentDate(), appointment.getTimeSlot());
            if (schedule != null && schedule.getBookedCount() > 0) {
                int maxRetries = 3;
                for (int i = 0; i < maxRetries; i++) {
                    int updated = doctorScheduleMapper.decrementBookedCountWithVersion(
                            appointment.getDoctorId(), appointment.getAppointmentDate(), appointment.getTimeSlot(),
                            schedule.getVersion());
                    if (updated > 0) {
                        break;
                    }
                    // 版本冲突，查询最新版本重试
                    schedule = doctorScheduleMapper.selectScheduleWithVersion(
                            appointment.getDoctorId(), appointment.getAppointmentDate(), appointment.getTimeSlot());
                    if (schedule == null || schedule.getBookedCount() <= 0) {
                        break;
                    }
                }
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
            List<DoctorSchedule> rawSchedules = aiAppointmentService.getDepartmentSchedulesFromCache(departmentId);
            List<Doctor> doctors = aiAppointmentService.getDoctorsByDepartmentFromCache(departmentId);
            Department dept = departmentMapper.selectById(departmentId);
            List<DoctorSchedule> schedules = rawSchedules.stream().map(s -> {
                DoctorSchedule copy = new DoctorSchedule();
                copy.setId(s.getId());
                copy.setDoctorId(s.getDoctorId());
                copy.setScheduleDate(s.getScheduleDate());
                copy.setTimeSlot(s.getTimeSlot());
                copy.setMaxPatients(s.getMaxPatients());
                copy.setBookedCount(s.getBookedCount());
                copy.setStatus(s.getStatus());
                doctors.stream()
                        .filter(d -> d.getId().equals(s.getDoctorId()))
                        .findFirst()
                        .ifPresent(d -> {
                            copy.setDoctorName(d.getName());
                            copy.setDepartmentId(departmentId);
                            copy.setDepartmentName(dept != null ? dept.getName() : "");
                        });
                return copy;
            }).toList();
            return Result.ok(schedules);
        }

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

    /**
     * 同步库存到Redis
     * 定时任务调用，将数据库中的号源库存同步到Redis缓存
     */
    @Override
    public Result syncStockToRedis() {
        try {
            // 查询未来7天所有可用排班
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = startDate.plusDays(7);
            QueryWrapper<DoctorSchedule> wrapper = new QueryWrapper<DoctorSchedule>()
                    .ge("schedule_date", startDate)
                    .le("schedule_date", endDate)
                    .eq("status", 1);
            List<DoctorSchedule> schedules = doctorScheduleMapper.selectList(wrapper);

            int count = 0;
            for (DoctorSchedule schedule : schedules) {
                String scheduleKey = buildScheduleKey(
                        schedule.getDoctorId(),
                        schedule.getScheduleDate().toString(),
                        schedule.getTimeSlot());
                int availableStock = schedule.getMaxPatients() - schedule.getBookedCount();
                scheduleStockService.initStockBatch(scheduleKey, availableStock);
                count++;
            }

            log.info("库存同步到Redis完成: 共{}条排班", count);
            return Result.ok("库存同步完成，共" + count + "条排班");
        } catch (Exception e) {
            log.error("库存同步失败: {}", e.getMessage());
            return Result.fail("库存同步失败: " + e.getMessage());
        }
    }

    /**
     * 构建锁Key
     */
    private String buildLockKey(Long doctorId, String scheduleDate, String timeSlot) {
        return APPOINTMENT_LOCK_PREFIX + doctorId + "_" + scheduleDate + "_" + timeSlot;
    }

    /**
     * 构建排班库存Key
     */
    private String buildScheduleKey(Long doctorId, String scheduleDate, String timeSlot) {
        return doctorId + "_" + scheduleDate + "_" + timeSlot;
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
