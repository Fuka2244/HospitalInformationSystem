package com.hospitalinfo.hospitalinformationsystem.service;

import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.dto.StatisticsQueryDto;
import com.hospitalinfo.hospitalinformationsystem.dto.SystemConfigDto;
import jakarta.servlet.http.HttpSession;

public interface IAdminService {
    Result getStatistics(StatisticsQueryDto queryDto, HttpSession session);
    Result getSystemConfigs(HttpSession session);
    Result updateSystemConfig(SystemConfigDto dto, HttpSession session);
    Result getUserManagement(HttpSession session);
    Result getDepartmentManagement(HttpSession session);
    Result getDoctorScheduleManagement(HttpSession session);
    Result getGlobalStatistics(HttpSession session);
    Result getPeriodicReport(StatisticsQueryDto queryDto, HttpSession session);
}
