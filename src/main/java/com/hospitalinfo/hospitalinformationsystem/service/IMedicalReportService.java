package com.hospitalinfo.hospitalinformationsystem.service;

import com.hospitalinfo.hospitalinformationsystem.dto.ReportGenerateDto;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;

/**
 * 医疗报告服务接口
 */
public interface IMedicalReportService {

    /** AI生成医疗报告 */
    Result generateReport(ReportGenerateDto dto, String patientId);

    /** 获取报告列表 */
    Result listReports(String patientId, String reportType, Integer page, Integer size);

    /** 获取报告详情 */
    Result getReportDetail(Long reportId);

    /** 导出PDF */
    Result exportPdf(Long reportId);

    /** 确认报告 */
    Result confirmReport(Long reportId);
}
