package com.hospitalinfo.hospitalinformationsystem.controller;

import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.dto.StatisticsQueryDto;
import com.hospitalinfo.hospitalinformationsystem.dto.SystemConfigDto;
import com.hospitalinfo.hospitalinformationsystem.service.IAdminService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final IAdminService adminService;

    @GetMapping("/statistics")
    public Result getStatistics(StatisticsQueryDto queryDto, HttpSession session) {
        return adminService.getStatistics(queryDto, session);
    }

    @GetMapping("/system-configs")
    public Result getSystemConfigs(HttpSession session) {
        return adminService.getSystemConfigs(session);
    }

    @PutMapping("/system-config")
    public Result updateSystemConfig(@RequestBody @Valid SystemConfigDto dto, HttpSession session) {
        return adminService.updateSystemConfig(dto, session);
    }

    @GetMapping("/user-management")
    public Result getUserManagement(HttpSession session) {
        return adminService.getUserManagement(session);
    }

    @GetMapping("/department-management")
    public Result getDepartmentManagement(HttpSession session) {
        return adminService.getDepartmentManagement(session);
    }

    @GetMapping("/doctor-schedule-management")
    public Result getDoctorScheduleManagement(HttpSession session) {
        return adminService.getDoctorScheduleManagement(session);
    }

    @GetMapping("/global-statistics")
    public Result getGlobalStatistics(HttpSession session) {
        return adminService.getGlobalStatistics(session);
    }

    @GetMapping("/periodic-report")
    public Result getPeriodicReport(StatisticsQueryDto queryDto, HttpSession session) {
        return adminService.getPeriodicReport(queryDto, session);
    }
}
