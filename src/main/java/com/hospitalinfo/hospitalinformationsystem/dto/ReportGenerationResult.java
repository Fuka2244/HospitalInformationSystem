package com.hospitalinfo.hospitalinformationsystem.dto;

import lombok.Data;

/**
 * AI报告生成结果
 */
@Data
public class ReportGenerationResult {
    private String summary;
    private String diagnosis;
    private String treatment;
    private String recommendation;
}
