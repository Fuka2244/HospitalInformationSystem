package com.hospitalinfo.hospitalinformationsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalinfo.hospitalinformationsystem.dto.ReportGenerateDto;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.service.IMedicalReportService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 医疗报告控制器（含AI生成 + PDF导出）
 */
@Slf4j
@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class MedicalReportController {

    private final IMedicalReportService reportService;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService = new ThreadPoolExecutor(
            2, 8, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100),
            new ThreadPoolExecutor.CallerRunsPolicy());

    /**
     * AI生成医疗报告（流式，实时展示思维链）
     * POST /report/generate-stream
     */
    @PostMapping("/generate-stream")
    public SseEmitter generateReportStream(@RequestBody ReportGenerateDto dto, HttpSession session) {
        String patientId = (String) session.getAttribute("account");
        SseEmitter emitter = new SseEmitter(120000L); // 120秒超时，AI生成可能需要较长时间

        executorService.submit(() -> {
            try {
                // 步骤1：开始分析，获取病历基本信息
                emitter.send(SseEmitter.event()
                        .name("step")
                        .data(objectMapper.writeValueAsString(buildStepData(1, "开始分析", "正在获取病历信息...", 0))));
                Thread.sleep(200);

                // 获取病历信息，准备展示实际数据
                com.hospitalinfo.hospitalinformationsystem.entity.MedicalRecord medicalRecord = null;
                if (dto.getMedicalRecordId() != null) {
                    medicalRecord = reportService.getMedicalRecordById(dto.getMedicalRecordId());
                }
                if (medicalRecord != null) {
                    String message = String.format("患者就诊日期：%s\n主诉：%s\n现病史：%s",
                            medicalRecord.getVisitDate(),
                            medicalRecord.getChiefComplaint(),
                            medicalRecord.getPresentIllness() != null &&
                                    medicalRecord.getPresentIllness().length() > 100 ?
                                    medicalRecord.getPresentIllness().substring(0, 100) + "..." :
                                    medicalRecord.getPresentIllness());
                    emitter.send(SseEmitter.event()
                            .name("step")
                            .data(objectMapper.writeValueAsString(buildStepData(1, "开始分析", message, 5))));
                }
                Thread.sleep(300);

                // 步骤2：分析主诉和现病史
                emitter.send(SseEmitter.event()
                        .name("step")
                        .data(objectMapper.writeValueAsString(buildStepData(2, "分析主诉和现病史", "正在提取关键症状信息...", 15))));
                if (medicalRecord != null) {
                    String message = String.format("关键症状提取：\n主诉：%s\n分析：患者主诉清晰，主要症状为%s",
                            medicalRecord.getChiefComplaint(),
                            extractMainSymptom(medicalRecord.getChiefComplaint()));
                    emitter.send(SseEmitter.event()
                            .name("step")
                            .data(objectMapper.writeValueAsString(buildStepData(2, "分析主诉和现病史", message, 20))));
                }
                Thread.sleep(300);

                // 步骤3：检查结果分析
                emitter.send(SseEmitter.event()
                        .name("step")
                        .data(objectMapper.writeValueAsString(buildStepData(3, "结合检查结果分析", "正在分析检查异常...", 35))));
                if (medicalRecord != null && dto.getExaminationData() != null && !dto.getExaminationData().isEmpty()) {
                    String message = String.format("检查结果分析：\n%s\n分析：检查结果%s，需要结合症状进一步分析",
                            dto.getExaminationData().length() > 150 ?
                                    dto.getExaminationData().substring(0, 150) + "..." :
                                    dto.getExaminationData(),
                            hasAbnormalResult(dto.getExaminationData()) ? "存在异常" : "基本正常");
                    emitter.send(SseEmitter.event()
                            .name("step")
                            .data(objectMapper.writeValueAsString(buildStepData(3, "结合检查结果分析", message, 40))));
                } else {
                    emitter.send(SseEmitter.event()
                            .name("step")
                            .data(objectMapper.writeValueAsString(buildStepData(3, "结合检查结果分析", "当前病历暂无检查数据，将主要依据临床症状进行分析", 40))));
                }
                Thread.sleep(300);

                // 步骤4：历史记录分析
                emitter.send(SseEmitter.event()
                        .name("step")
                        .data(objectMapper.writeValueAsString(buildStepData(4, "参考历史记录", "正在查看历史就诊记录...", 55))));
                String historyInfo = reportService.getPatientHistory(patientId);
                if (historyInfo != null && !historyInfo.isEmpty()) {
                    String message = String.format("历史就诊记录：\n%s\n分析：%s",
                            historyInfo.length() > 200 ? historyInfo.substring(0, 200) + "..." : historyInfo,
                            analyzeHistory(historyInfo));
                    emitter.send(SseEmitter.event()
                            .name("step")
                            .data(objectMapper.writeValueAsString(buildStepData(4, "参考历史记录", message, 60))));
                } else {
                    emitter.send(SseEmitter.event()
                            .name("step")
                            .data(objectMapper.writeValueAsString(buildStepData(4, "参考历史记录", "患者无历史就诊记录，本次为初诊或首诊", 60))));
                }
                Thread.sleep(300);

                // 步骤5：综合诊断
                emitter.send(SseEmitter.event()
                        .name("step")
                        .data(objectMapper.writeValueAsString(buildStepData(5, "综合分析诊断", "正在得出诊断结论...", 75))));
                if (medicalRecord != null) {
                    String message = String.format("初步诊断：%s\n结合主诉、现病史、检查结果及历史记录，初步诊断为：%s",
                            medicalRecord.getDiagnosis(),
                            medicalRecord.getDiagnosis());
                    emitter.send(SseEmitter.event()
                            .name("step")
                            .data(objectMapper.writeValueAsString(buildStepData(5, "综合分析诊断", message, 80))));
                }
                Thread.sleep(300);

                // 步骤6：治疗方案
                emitter.send(SseEmitter.event()
                        .name("step")
                        .data(objectMapper.writeValueAsString(buildStepData(6, "提出治疗方案和建议", "正在生成详细建议...", 90))));

                // 生成报告
                Result result = reportService.generateReport(dto, patientId);

                if (result.getSuccess() != null && result.getSuccess()) {
                    // 发送思维链详情
                    Object report = result.getData();
                    if (report instanceof com.hospitalinfo.hospitalinformationsystem.entity.MedicalReport) {
                        com.hospitalinfo.hospitalinformationsystem.entity.MedicalReport mr =
                                (com.hospitalinfo.hospitalinformationsystem.entity.MedicalReport) report;

                        // 步骤6：展示治疗方案
                        if (mr.getAiTreatment() != null && !mr.getAiTreatment().isEmpty()) {
                            String treatmentMessage = String.format("治疗方案：\n%s\n康复建议：\n%s",
                                    mr.getAiTreatment().length() > 200 ?
                                            mr.getAiTreatment().substring(0, 200) + "..." :
                                            mr.getAiTreatment(),
                                    mr.getAiRecommendation() != null && !mr.getAiRecommendation().isEmpty() ?
                                            mr.getAiRecommendation().length() > 150 ?
                                                    mr.getAiRecommendation().substring(0, 150) + "..." :
                                                    mr.getAiRecommendation() :
                                            "详见完整报告");
                            emitter.send(SseEmitter.event()
                                    .name("step")
                                    .data(objectMapper.writeValueAsString(buildStepData(6, "提出治疗方案和建议", treatmentMessage, 95))));
                        }

                        // 发送思维链
                        String thoughtChain = mr.getAiThoughtChain() != null ? mr.getAiThoughtChain() : "";
                        emitter.send(SseEmitter.event()
                                .name("thoughtchain")
                                .data(thoughtChain));

                        // 发送完成事件，包含完整报告
                        emitter.send(SseEmitter.event()
                                .name("complete")
                                .data(report));
                    } else {
                        emitter.send(SseEmitter.event()
                                .name("complete")
                                .data(result.getData()));
                    }
                } else {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(result.getErrorMsg()));
                }

                // 完成
                emitter.send(SseEmitter.event()
                        .name("finish")
                        .data("{\"progress\":100}"));
                emitter.complete();

            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("生成失败: " + e.getMessage()));
                    emitter.completeWithError(e);
                } catch (IOException ioException) {
                    emitter.completeWithError(ioException);
                }
            }
        });

        return emitter;
    }

    private String extractMainSymptom(String chiefComplaint) {
        if (chiefComplaint == null) return "无明显症状";
        if (chiefComplaint.contains("头痛")) return "头痛";
        if (chiefComplaint.contains("发热")) return "发热";
        if (chiefComplaint.contains("咳嗽")) return "咳嗽";
        if (chiefComplaint.contains("腹痛")) return "腹痛";
        if (chiefComplaint.contains("胸痛")) return "胸痛";
        if (chiefComplaint.contains("头晕")) return "头晕";
        if (chiefComplaint.contains("恶心")) return "恶心";
        return chiefComplaint.substring(0, Math.min(chiefComplaint.length(), 10));
    }

    private boolean hasAbnormalResult(String examinationData) {
        if (examinationData == null) return false;
        return examinationData.contains("异常") ||
                examinationData.contains("偏高") ||
                examinationData.contains("偏低") ||
                examinationData.contains("阳性") ||
                examinationData.contains("＞") ||
                examinationData.contains("＜");
    }

    private String analyzeHistory(String historyInfo) {
        if (historyInfo == null || historyInfo.isEmpty()) return "无历史记录";
        if (historyInfo.contains("高血压")) return "患者有高血压病史，需持续关注血压控制情况";
        if (historyInfo.contains("糖尿病")) return "患者有糖尿病病史，需监测血糖水平";
        if (historyInfo.contains("慢性")) return "患者有慢性病史，本次可能是慢性病复发或加重";
        return "患者有就诊历史，需对比分析病情变化";
    }

    /**
     * 构建SSE步骤数据（使用Map确保ObjectMapper安全序列化，避免JSON注入）
     */
    private Map<String, Object> buildStepData(int step, String title, String message, int progress) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("step", step);
        data.put("title", title);
        data.put("message", message);
        data.put("progress", progress);
        return data;
    }

    /**
     * AI生成医疗报告（非流式）
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
    public Result getReportDetail(@PathVariable Long id, HttpSession session) {
        String currentPatientId = (String) session.getAttribute("account");
        Object role = session.getAttribute("role");
        return reportService.getReportDetail(id, currentPatientId, role);
    }

    /**
     * 导出PDF
     * GET /report/{id}/export-pdf
     */
    @GetMapping("/{id}/export-pdf")
    public void exportPdf(@PathVariable Long id, HttpServletResponse response, HttpSession session) throws IOException {
        String currentPatientId = (String) session.getAttribute("account");
        Object role = session.getAttribute("role");

        // 权限验证：先检查是否有权访问该报告
        Result accessCheck = reportService.checkReportAccess(id, currentPatientId, role);
        if (!accessCheck.getSuccess()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"errorMsg\":\"" + accessCheck.getErrorMsg() + "\"}");
            return;
        }

        String pdfPath = reportService.exportPdf(id);

        if (pdfPath == null || pdfPath.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Path path = Paths.get(pdfPath).toAbsolutePath().normalize();

        // 防止路径遍历攻击：确保PDF路径在允许的目录内
        Path allowedDir = Paths.get("reports/pdf").toAbsolutePath().normalize();
        if (!path.startsWith(allowedDir)) {
            log.warn("PDF路径遍历攻击嫌疑: {}", pdfPath);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (!Files.exists(path)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 设置响应头
        String filename = path.getFileName().toString();
        response.setContentType("application/pdf");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        // 写入文件内容
        Files.copy(path, response.getOutputStream());
        response.getOutputStream().flush();
    }

    /**
     * 确认报告
     * PUT /report/{id}/confirm
     */
    @PutMapping("/{id}/confirm")
    public Result confirmReport(@PathVariable Long id, HttpSession session) {
        String currentPatientId = (String) session.getAttribute("account");
        Object role = session.getAttribute("role");
        return reportService.confirmReport(id, currentPatientId, role);
    }
}
