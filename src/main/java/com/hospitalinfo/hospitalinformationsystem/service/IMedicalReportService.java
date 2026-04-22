package com.hospitalinfo.hospitalinformationsystem.service;

import com.hospitalinfo.hospitalinformationsystem.dto.ReportGenerateDto;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.entity.MedicalRecord;

/**
 * 医疗报告服务接口
 */
public interface IMedicalReportService {

    /** AI生成医疗报告 */
    Result generateReport(ReportGenerateDto dto, String patientId);

    /** 获取报告列表 */
    Result listReports(String patientId, String reportType, Integer page, Integer size);

    /** 获取报告详情（含权限验证） */
    Result getReportDetail(Long reportId, String currentPatientId, Object role);

    /** 检查报告访问权限 */
    Result checkReportAccess(Long reportId, String currentPatientId, Object role);

    /** 导出PDF - 返回PDF文件路径 */
    String exportPdf(Long reportId);

    /** 确认报告（含权限验证） */
    Result confirmReport(Long reportId, String currentPatientId, Object role);

    /** 获取病历记录 */
    MedicalRecord getMedicalRecordById(Long id);

    /** 获取患者历史记录摘要 */
    String getPatientHistory(String patientId);
}
