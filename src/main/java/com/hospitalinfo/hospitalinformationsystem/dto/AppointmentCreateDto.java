package com.hospitalinfo.hospitalinformationsystem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建预约请求
 */
@Data
public class AppointmentCreateDto {
    @NotNull(message = "科室ID不能为空")
    private Long departmentId;
    private Long doctorId;
    @NotNull(message = "预约类型不能为空")
    private String appointmentType; // DOCTOR / EXAMINATION
    private String examinationType;  // CT, MRI 等
    @NotNull(message = "预约日期不能为空")
    private LocalDate appointmentDate;
    @NotNull(message = "时间段不能为空")
    private String timeSlot;
}
