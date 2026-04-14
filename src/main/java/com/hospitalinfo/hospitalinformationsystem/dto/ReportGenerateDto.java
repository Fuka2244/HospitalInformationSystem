package com.hospitalinfo.hospitalinformationsystem.dto;

import lombok.Data;

/**
 * 医疗报告生成请求
 */
@Data
public class ReportGenerateDto {
    private Long medicalRecordId;
    private String reportType;   // EXAMINATION / TREATMENT
    private String title;
    private String examinationData;
}
