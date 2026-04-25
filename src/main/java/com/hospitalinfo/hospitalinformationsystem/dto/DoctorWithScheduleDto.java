package com.hospitalinfo.hospitalinformationsystem.dto;

import com.hospitalinfo.hospitalinformationsystem.entity.DoctorSchedule;
import lombok.Data;

import java.util.List;

/**
 * 医生及其可用排班（用于导诊推荐返回可选医生列表）
 */
@Data
public class DoctorWithScheduleDto {
    private Long doctorId;
    private String doctorName;
    private String title;
    private String specialty;
    private List<DoctorSchedule> schedules;
}
