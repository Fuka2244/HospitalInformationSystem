package com.hospitalinfo.hospitalinformationsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.dto.StatisticsQueryDto;
import com.hospitalinfo.hospitalinformationsystem.dto.SystemConfigDto;
import com.hospitalinfo.hospitalinformationsystem.entity.*;
import com.hospitalinfo.hospitalinformationsystem.mapper.*;
import com.hospitalinfo.hospitalinformationsystem.service.IAdminService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements IAdminService {

    private final AppointmentMapper appointmentMapper;
    private final PatientMapper patientMapper;
    private final DoctorMapper doctorMapper;
    private final DepartmentMapper departmentMapper;
    private final MedicineInventoryMapper medicineInventoryMapper;
    private final BillingMapper billingMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final PharmacistMapper pharmacistMapper;

    @Override
    public Result getStatistics(StatisticsQueryDto queryDto, HttpSession session) {
        String role = (String) session.getAttribute("role");
        
        if (!"admin".equals(role)) {
            return Result.fail("无权访问统计功能");
        }

        LocalDate startDate = queryDto.getStartDate() != null ? queryDto.getStartDate() : LocalDate.now().minusDays(30);
        LocalDate endDate = queryDto.getEndDate() != null ? queryDto.getEndDate() : LocalDate.now();

        Map<String, Object> statistics = new HashMap<>();

        QueryWrapper<Appointment> appointmentWrapper = new QueryWrapper<>();
        appointmentWrapper.between("create_time", startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
        if (queryDto.getDepartmentId() != null) {
            appointmentWrapper.eq("department_id", queryDto.getDepartmentId());
        }
        if (queryDto.getDoctorId() != null) {
            appointmentWrapper.eq("doctor_id", queryDto.getDoctorId());
        }
        List<Appointment> appointments = appointmentMapper.selectList(appointmentWrapper);
        statistics.put("totalAppointments", appointments.size());
        statistics.put("completedAppointments", appointments.stream().filter(a -> a.getStatus() == 3).count());
        statistics.put("cancelledAppointments", appointments.stream().filter(a -> a.getStatus() == -1).count());

        QueryWrapper<Billing> billingWrapper = new QueryWrapper<>();
        billingWrapper.between("create_time", startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
        List<Billing> billings = billingMapper.selectList(billingWrapper);
        statistics.put("totalRevenue", billings.stream().mapToDouble(Billing::getAmount).sum());
        statistics.put("paidAmount", billings.stream().filter(b -> b.getStatus() == 1).mapToDouble(Billing::getAmount).sum());

        QueryWrapper<Patient> patientWrapper = new QueryWrapper<>();
        patientWrapper.between("create_time", startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
        statistics.put("newPatients", patientMapper.selectCount(patientWrapper));

        return Result.ok(statistics);
    }

    @Override
    public Result getSystemConfigs(HttpSession session) {
        String role = (String) session.getAttribute("role");
        
        if (!"admin".equals(role)) {
            return Result.fail("无权访问系统配置");
        }

        QueryWrapper<SystemConfig> wrapper = new QueryWrapper<>();
        List<SystemConfig> configs = systemConfigMapper.selectList(wrapper);
        return Result.ok(configs);
    }

    @Override
    public Result updateSystemConfig(SystemConfigDto dto, HttpSession session) {
        String role = (String) session.getAttribute("role");
        
        if (!"admin".equals(role)) {
            return Result.fail("无权修改系统配置");
        }

        SystemConfig config = systemConfigMapper.selectById(dto.getId());
        if (config == null) {
            return Result.fail("配置不存在");
        }

        config.setConfigKey(dto.getConfigKey());
        config.setConfigValue(dto.getConfigValue());
        config.setConfigType(dto.getConfigType());
        config.setDescription(dto.getDescription());
        config.setUpdateTime(LocalDateTime.now());
        systemConfigMapper.updateById(config);

        return Result.ok("配置更新成功");
    }

    @Override
    public Result getUserManagement(HttpSession session) {
        String role = (String) session.getAttribute("role");
        
        if (!"admin".equals(role)) {
            return Result.fail("无权访问用户管理");
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("patients", patientMapper.selectList(null));
        userData.put("doctors", doctorMapper.selectList(null));
        userData.put("pharmacists", pharmacistMapper.selectList(null));

        return Result.ok(userData);
    }

    @Override
    public Result getDepartmentManagement(HttpSession session) {
        String role = (String) session.getAttribute("role");
        
        if (!"admin".equals(role)) {
            return Result.fail("无权访问科室管理");
        }

        List<Department> departments = departmentMapper.selectList(null);
        return Result.ok(departments);
    }

    @Override
    public Result getDoctorScheduleManagement(HttpSession session) {
        String role = (String) session.getAttribute("role");
        
        if (!"admin".equals(role)) {
            return Result.fail("无权访问排班管理");
        }

        Map<String, Object> scheduleData = new HashMap<>();
        scheduleData.put("doctors", doctorMapper.selectList(null));
        scheduleData.put("departments", departmentMapper.selectList(null));

        return Result.ok(scheduleData);
    }

    @Override
    public Result getGlobalStatistics(HttpSession session) {
        String role = (String) session.getAttribute("role");
        
        if (!"admin".equals(role)) {
            return Result.fail("无权访问全局统计");
        }

        Map<String, Object> globalStats = new HashMap<>();

        globalStats.put("totalPatients", patientMapper.selectCount(null));
        globalStats.put("totalDoctors", doctorMapper.selectCount(null));
        globalStats.put("totalDepartments", departmentMapper.selectCount(null));
        globalStats.put("totalMedicines", medicineInventoryMapper.selectCount(null));

        QueryWrapper<Appointment> appointmentWrapper = new QueryWrapper<>();
        appointmentWrapper.eq("appointment_date", LocalDate.now());
        globalStats.put("todayAppointments", appointmentMapper.selectCount(appointmentWrapper));

        QueryWrapper<Billing> billingWrapper = new QueryWrapper<>();
        billingWrapper.eq("status", 0);
        globalStats.put("unpaidBills", billingMapper.selectCount(billingWrapper));

        return Result.ok(globalStats);
    }
}