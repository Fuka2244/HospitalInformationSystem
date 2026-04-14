package com.hospitalinfo.hospitalinformationsystem.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 就诊记录VO
 */
@Data
public class VisitRecordVo {
    private Long recordId;
    private LocalDateTime visitDate;
    private String doctorName;
    private String departmentName;
    private String diagnosis;
    private String prescriptionSummary;
}
