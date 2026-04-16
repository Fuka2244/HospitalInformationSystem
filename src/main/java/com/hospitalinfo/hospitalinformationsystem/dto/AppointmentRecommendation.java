package com.hospitalinfo.hospitalinformationsystem.dto;

import lombok.Data;

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
}
