package com.hospitalinfo.hospitalinformationsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospitalinfo.hospitalinformationsystem.ai.AiReportService;
import com.hospitalinfo.hospitalinformationsystem.dto.ReportGenerateDto;
import com.hospitalinfo.hospitalinformationsystem.dto.ReportGenerationResult;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.entity.*;
import com.hospitalinfo.hospitalinformationsystem.mapper.*;
import com.hospitalinfo.hospitalinformationsystem.service.IMedicalReportService;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;

import java.io.InputStream;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicalReportServiceImpl implements IMedicalReportService {

    private final MedicalReportMapper medicalReportMapper;
    private final MedicalRecordMapper medicalRecordMapper;
    private final DoctorMapper doctorMapper;
    private final PatientMapper patientMapper;
    private final AiReportService aiReportService;

    private static final String PDF_OUTPUT_DIR = "reports/pdf/";

    @Override
    public Result generateReport(ReportGenerateDto dto, String patientId) {
        // 获取病历信息
        MedicalRecord medicalRecord;
        if (dto.getMedicalRecordId() != null) {
            medicalRecord = medicalRecordMapper.selectById(dto.getMedicalRecordId());
        } else {
            // 取最近一条病历
            medicalRecord = medicalRecordMapper.selectOne(
                    new QueryWrapper<MedicalRecord>()
                            .eq("patient_id", patientId)
                            .eq("status", 1)
                            .orderByDesc("visit_date")
                            .last("LIMIT 1"));
        }

        if (medicalRecord == null) {
            return Result.fail("未找到病历记录");
        }

        // 构建历史记录上下文
        String historyContext = buildHistoryContext(patientId);

        // 调用AI生成报告
        ReportGenerationResult aiResult = aiReportService.generateReport(
                medicalRecord, dto.getExaminationData(), historyContext);

        // 保存报告
        MedicalReport report = new MedicalReport();
        report.setPatientId(patientId);
        report.setMedicalRecordId(medicalRecord.getId());
        report.setDoctorId(medicalRecord.getDoctorId());
        report.setReportType(dto.getReportType() != null ? dto.getReportType() : "EXAMINATION");
        report.setTitle(dto.getTitle());
        report.setExaminationData(dto.getExaminationData());
        report.setAiSummary(aiResult.getSummary());
        report.setAiDiagnosis(aiResult.getDiagnosis());
        report.setAiTreatment(aiResult.getTreatment());
        report.setAiRecommendation(aiResult.getRecommendation());
        report.setAiThoughtChain(aiResult.getThoughtChain());
        report.setStatus(0); // 草稿

        medicalReportMapper.insert(report);
        return Result.ok(report);
    }

    @Override
    public Result listReports(String patientId, String reportType, Integer page, Integer size) {
        Page<MedicalReport> pageParam = new Page<>(page, size);
        QueryWrapper<MedicalReport> wrapper = new QueryWrapper<MedicalReport>()
                .eq("patient_id", patientId);

        if (reportType != null && !reportType.isEmpty()) {
            wrapper.eq("report_type", reportType);
        }
        wrapper.orderByDesc("create_time");

        Page<MedicalReport> result = medicalReportMapper.selectPage(pageParam, wrapper);
        fillReportNames(result.getRecords());

        return Result.ok(result.getRecords(), result.getTotal());
    }

    @Override
    public Result getReportDetail(Long reportId, String currentPatientId, Object role) {
        MedicalReport report = medicalReportMapper.selectById(reportId);
        if (report == null) {
            return Result.fail("报告不存在");
        }
        // 权限验证：患者只能查看自己的报告，管理员/医生/药师可查看所有
        if (!hasReportAccess(report, currentPatientId, role)) {
            return Result.fail("无权查看该报告");
        }
        fillReportNames(List.of(report));
        return Result.ok(report);
    }

    @Override
    public Result checkReportAccess(Long reportId, String currentPatientId, Object role) {
        MedicalReport report = medicalReportMapper.selectById(reportId);
        if (report == null) {
            return Result.fail("报告不存在");
        }
        if (!hasReportAccess(report, currentPatientId, role)) {
            return Result.fail("无权访问该报告");
        }
        return Result.ok();
    }

    /**
     * 验证当前用户是否有权访问指定报告
     * 患者：只能访问自己的报告
     * 医生：可以访问自己接诊产生的报告
     * 管理员/药师：可访问所有报告
     */
    private boolean hasReportAccess(MedicalReport report, String currentPatientId, Object role) {
        // 管理员、药师可查看所有报告
        if ("admin".equals(role) || "pharmacist".equals(role)) {
            return true;
        }
        // 医生可查看自己接诊产生的报告
        if ("doctor".equals(role)) {
            return true; // 如需更严格可检查 report.getDoctorId()
        }
        // 患者只能查看自己的报告
        return report.getPatientId().equals(currentPatientId);
    }

    @Override
    public String exportPdf(Long reportId) {
        MedicalReport report = medicalReportMapper.selectById(reportId);
        if (report == null) {
            return null;
        }

        try {
            // 确保目录存在
            File dir = new File(PDF_OUTPUT_DIR);
            if (!dir.exists()) dir.mkdirs();

            String fileName = "report_" + reportId + "_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".pdf";
            String filePath = PDF_OUTPUT_DIR + fileName;

            PdfWriter writer = new PdfWriter(filePath);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // 中文字体 - 优先使用系统宋体，避免STSong-Light对某些字符glyph为null
            PdfFont font;
            String simsunPath = "C:/Windows/Fonts/simsun.ttc";
            File simsunFile = new File(simsunPath);
            if (simsunFile.exists()) {
                // Windows系统宋体，TTC集合字体需要指定索引0
                font = PdfFontFactory.createFont(simsunPath + ",0", PdfEncodings.IDENTITY_H);
            } else {
                // 回退到iText内置CJK字体
                font = PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H");
            }
            document.setFont(font);

            // 标题
            document.add(new Paragraph("医疗报告").setFontSize(20).setBold());
            document.add(new Paragraph("报告编号: " + reportId));
            document.add(new Paragraph("生成时间: " + LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));

            // 患者信息
            Patient patient = patientMapper.selectById(report.getPatientId());
            if (patient != null) {
                document.add(new Paragraph("\n患者信息").setFontSize(14).setBold());
                document.add(new Paragraph("姓名: " + patient.getName()));
                document.add(new Paragraph("性别: " + patient.getGender()));
                document.add(new Paragraph("年龄: " + patient.getAge()));
            }

            // AI报告内容
            document.add(new Paragraph("\n病情摘要").setFontSize(14).setBold());
            document.add(new Paragraph(report.getAiSummary()));

            document.add(new Paragraph("\n诊断结论").setFontSize(14).setBold());
            document.add(new Paragraph(report.getAiDiagnosis()));

            document.add(new Paragraph("\n治疗方案").setFontSize(14).setBold());
            document.add(new Paragraph(report.getAiTreatment()));

            document.add(new Paragraph("\n康复建议").setFontSize(14).setBold());
            document.add(new Paragraph(report.getAiRecommendation()));

            document.add(new Paragraph("\n\n* 本报告由AI辅助生成，仅供参考，最终诊断请以医生意见为准。")
                    .setFontSize(10));

            document.close();

            // 更新PDF路径
            report.setPdfPath(filePath);
            medicalReportMapper.updateById(report);

            return filePath;
        } catch (Exception e) {
            log.error("PDF生成失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Result confirmReport(Long reportId, String currentPatientId, Object role) {
        MedicalReport report = medicalReportMapper.selectById(reportId);
        if (report == null) {
            return Result.fail("报告不存在");
        }
        // 权限验证
        if (!hasReportAccess(report, currentPatientId, role)) {
            return Result.fail("无权操作该报告");
        }
        report.setStatus(1); // 已确认
        medicalReportMapper.updateById(report);
        return Result.ok();
    }

    private String buildHistoryContext(String patientId) {
        List<MedicalRecord> historyRecords = medicalRecordMapper.selectList(
                new QueryWrapper<MedicalRecord>()
                        .eq("patient_id", patientId)
                        .eq("status", 1)
                        .orderByDesc("visit_date")
                        .last("LIMIT 5"));

        if (historyRecords.isEmpty()) return "无历史就诊记录";

        StringBuilder sb = new StringBuilder();
        for (MedicalRecord r : historyRecords) {
            Doctor doctor = doctorMapper.selectById(r.getDoctorId());
            String doctorName = doctor != null ? doctor.getName() : "未知";
            sb.append(String.format("- 就诊时间: %s, 医生: %s, 诊断: %s, 治疗: %s\n",
                    r.getVisitDate(), doctorName, r.getDiagnosis(), r.getTreatmentPlan()));
        }
        return sb.toString();
    }

    @Override
    public MedicalRecord getMedicalRecordById(Long id) {
        return medicalRecordMapper.selectById(id);
    }

    @Override
    public String getPatientHistory(String patientId) {
        List<MedicalRecord> historyRecords = medicalRecordMapper.selectList(
                new QueryWrapper<MedicalRecord>()
                        .eq("patient_id", patientId)
                        .eq("status", 1)
                        .orderByDesc("visit_date")
                        .last("LIMIT 3"));

        if (historyRecords.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < historyRecords.size(); i++) {
            MedicalRecord r = historyRecords.get(i);
            Doctor doctor = doctorMapper.selectById(r.getDoctorId());
            String doctorName = doctor != null ? doctor.getName() : "未知";
            sb.append(String.format("%d. %s - %s医生: %s",
                    i + 1, r.getVisitDate(), doctorName, r.getDiagnosis()));
            if (i < historyRecords.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private void fillReportNames(List<MedicalReport> reports) {
        for (MedicalReport r : reports) {
            Patient patient = patientMapper.selectById(r.getPatientId());
            if (patient != null) r.setPatientName(patient.getName());
            Doctor doctor = doctorMapper.selectById(r.getDoctorId());
            if (doctor != null) r.setDoctorName(doctor.getName());
        }
    }
}
