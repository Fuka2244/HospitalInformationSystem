package com.hospitalinfo.hospitalinformationsystem.dto;

import lombok.Data;

import java.util.List;

/**
 * AI智能预约推荐结果
 */
@Data
public class AppointmentRecommendation {
    private String department;
    private Long departmentId;
    private String doctor;
    private Long doctorId;
    private String recommendedTime;
    private String recommendedDate;
    private String reason;

    /** 该科室下所有有排班的医生列表（供患者选择） */
    private List<DoctorWithScheduleDto> availableDoctors;

    /** 是否需要患者选择医生（当推荐医生无排班或有多位可选时为true） */
    private boolean needChooseDoctor;
}
