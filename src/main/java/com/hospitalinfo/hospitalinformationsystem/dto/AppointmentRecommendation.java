package com.hospitalinfo.hospitalinformationsystem.dto;

import lombok.Data;

/**
 * AI智能预约推荐结果
 */
@Data
public class AppointmentRecommendation {
    private String department;
    private String doctor;
    private String recommendedTime;
    private String reason;
}
