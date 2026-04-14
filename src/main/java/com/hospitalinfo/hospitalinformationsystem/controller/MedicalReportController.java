package com.hospitalinfo.hospitalinformationsystem.controller;

import com.hospitalinfo.hospitalinformationsystem.dto.ReportGenerateDto;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.service.IMedicalReportService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 医疗报告控制器（含AI生成 + PDF导出）
 */
@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class MedicalReportController {

    private final IMedicalReportService reportService;

    /**
     * AI生成医疗报告
     * POST /report/generate
     */
    @PostMapping("/generate")
    public Result generateReport(@RequestBody ReportGenerateDto dto, HttpSession session) {
        String patientId = (String) session.getAttribute("account");
        return reportService.generateReport(dto, patientId);
    }

    /**
     * 查询报告列表
     * GET /report/list?reportType=EXAMINATION&page=1&size=10
     */
    @GetMapping("/list")
    public Result listReports(@RequestParam(required = false) String reportType,
                               @RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer size,
                               HttpSession session) {
        String patientId = (String) session.getAttribute("account");
        return reportService.listReports(patientId, reportType, page, size);
    }

    /**
     * 获取报告详情
     * GET /report/{id}
     */
    @GetMapping("/{id}")
    public Result getReportDetail(@PathVariable Long id) {
        return reportService.getReportDetail(id);
    }

    /**
     * 导出PDF
     * GET /report/{id}/export-pdf
     */
    @GetMapping("/{id}/export-pdf")
    public Result exportPdf(@PathVariable Long id) {
        return reportService.exportPdf(id);
    }

    /**
     * 确认报告
     * PUT /report/{id}/confirm
     */
    @PutMapping("/{id}/confirm")
    public Result confirmReport(@PathVariable Long id) {
        return reportService.confirmReport(id);
    }
}
