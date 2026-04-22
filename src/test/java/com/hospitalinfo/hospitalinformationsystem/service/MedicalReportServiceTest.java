package com.hospitalinfo.hospitalinformationsystem.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospitalinfo.hospitalinformationsystem.ai.AiReportService;
import com.hospitalinfo.hospitalinformationsystem.dto.ReportGenerateDto;
import com.hospitalinfo.hospitalinformationsystem.dto.ReportGenerationResult;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.entity.*;
import com.hospitalinfo.hospitalinformationsystem.mapper.*;
import com.hospitalinfo.hospitalinformationsystem.service.impl.MedicalReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 医疗报告模块单元测试
 * 覆盖：AI生成报告、报告列表、报告详情、导出PDF、确认报告
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("模块5 - 医疗报告生成")
class MedicalReportServiceTest {

    @Mock private MedicalReportMapper medicalReportMapper;
    @Mock private MedicalRecordMapper medicalRecordMapper;
    @Mock private DoctorMapper doctorMapper;
    @Mock private PatientMapper patientMapper;
    @Mock private AiReportService aiReportService;

    @InjectMocks
    private MedicalReportServiceImpl reportService;

    private MedicalRecord mockRecord;
    private MedicalReport mockReport;
    private Patient mockPatient;
    private Doctor mockDoctor;

    @BeforeEach
    void setUp() {
        mockRecord = new MedicalRecord();
        mockRecord.setId(1L);
        mockRecord.setPatientId("patient-001");
        mockRecord.setDoctorId(1L);
        mockRecord.setDepartmentId(1L);
        mockRecord.setChiefComplaint("反复头痛1周");
        mockRecord.setPresentIllness("患者1周前无明显诱因出现头痛");
        mockRecord.setDiagnosis("偏头痛");
        mockRecord.setTreatmentPlan("止痛+休息");
        mockRecord.setVisitDate(LocalDateTime.of(2025, 3, 10, 9, 0));
        mockRecord.setStatus(1);

        mockReport = new MedicalReport();
        mockReport.setId(1L);
        mockReport.setPatientId("patient-001");
        mockReport.setMedicalRecordId(1L);
        mockReport.setDoctorId(1L);
        mockReport.setReportType("EXAMINATION");
        mockReport.setTitle("头痛检查报告");
        mockReport.setExaminationData("头颅CT: 未见明显异常");
        mockReport.setAiSummary("患者反复头痛1周，CT未见异常");
        mockReport.setAiDiagnosis("偏头痛（无先兆型）");
        mockReport.setAiTreatment("布洛芬止痛+生活方式调整");
        mockReport.setAiRecommendation("规律作息、避免诱因、3个月后复查");
        mockReport.setStatus(0);

        mockPatient = new Patient();
        mockPatient.setAccount("patient-001");
        mockPatient.setName("张三");
        mockPatient.setGender("男");
        mockPatient.setAge(35);

        mockDoctor = new Doctor();
        mockDoctor.setId(1L);
        mockDoctor.setName("李医生");
    }

    // ==================== AI生成报告 ====================

    @Nested
    @DisplayName("AI生成医疗报告")
    class GenerateReportTests {

        @Test
        @DisplayName("生成报告成功 - 指定病历ID")
        void generateWithRecordId() {
            ReportGenerateDto dto = new ReportGenerateDto();
            dto.setMedicalRecordId(1L);
            dto.setReportType("EXAMINATION");
            dto.setTitle("头痛检查报告");
            dto.setExaminationData("头颅CT: 未见明显异常");

            ReportGenerationResult aiResult = new ReportGenerationResult();
            aiResult.setSummary("患者反复头痛1周");
            aiResult.setDiagnosis("偏头痛");
            aiResult.setTreatment("止痛+休息");
            aiResult.setRecommendation("规律作息");

            when(medicalRecordMapper.selectById(1L)).thenReturn(mockRecord);
            when(medicalRecordMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(mockRecord));
            when(doctorMapper.selectById(1L)).thenReturn(mockDoctor);
            when(aiReportService.generateReport(any(MedicalRecord.class), anyString(), anyString()))
                    .thenReturn(aiResult);
            when(medicalReportMapper.insert(any(MedicalReport.class))).thenReturn(1);

            Result result = reportService.generateReport(dto, "patient-001");

            assertTrue(result.getSuccess());
            verify(medicalReportMapper).insert(any(MedicalReport.class));
        }

        @Test
        @DisplayName("生成报告成功 - 不指定病历ID自动取最近")
        void generateWithoutRecordId() {
            ReportGenerateDto dto = new ReportGenerateDto();
            dto.setReportType("TREATMENT");
            dto.setTitle("治疗报告");
            dto.setExaminationData(null);

            ReportGenerationResult aiResult = new ReportGenerationResult();
            aiResult.setSummary("摘要");
            aiResult.setDiagnosis("诊断");
            aiResult.setTreatment("治疗");
            aiResult.setRecommendation("建议");

            when(medicalRecordMapper.selectOne(any(QueryWrapper.class))).thenReturn(mockRecord);
            when(medicalRecordMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(mockRecord));
            when(doctorMapper.selectById(1L)).thenReturn(mockDoctor);
            when(aiReportService.generateReport(any(MedicalRecord.class), any(), anyString()))
                    .thenReturn(aiResult);
            when(medicalReportMapper.insert(any(MedicalReport.class))).thenReturn(1);

            Result result = reportService.generateReport(dto, "patient-001");

            assertTrue(result.getSuccess());
        }

        @Test
        @DisplayName("生成报告失败 - 病历不存在")
        void generateFailNoRecord() {
            ReportGenerateDto dto = new ReportGenerateDto();
            dto.setMedicalRecordId(999L);

            when(medicalRecordMapper.selectById(999L)).thenReturn(null);

            Result result = reportService.generateReport(dto, "patient-001");

            assertFalse(result.getSuccess());
            assertEquals("未找到病历记录", result.getErrorMsg());
        }

        @Test
        @DisplayName("生成报告 - 无历史记录时上下文为'无历史就诊记录'")
        void generateWithNoHistory() {
            ReportGenerateDto dto = new ReportGenerateDto();
            dto.setMedicalRecordId(1L);

            ReportGenerationResult aiResult = new ReportGenerationResult();
            aiResult.setSummary("摘要");
            aiResult.setDiagnosis("诊断");
            aiResult.setTreatment("治疗");
            aiResult.setRecommendation("建议");

            when(medicalRecordMapper.selectById(1L)).thenReturn(mockRecord);
            when(medicalRecordMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of());
            when(aiReportService.generateReport(any(MedicalRecord.class), any(), eq("无历史就诊记录")))
                    .thenReturn(aiResult);
            when(medicalReportMapper.insert(any(MedicalReport.class))).thenReturn(1);

            Result result = reportService.generateReport(dto, "patient-001");

            assertTrue(result.getSuccess());
        }
    }

    // ==================== 报告列表 ====================

    @Nested
    @DisplayName("报告列表查询")
    class ListReportsTests {

        @Test
        @DisplayName("查询报告列表 - 填充名称信息")
        void listReportsSuccess() {
            Page<MedicalReport> page = new Page<>(1, 10);
            page.setRecords(List.of(mockReport));
            page.setTotal(1L);

            when(medicalReportMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(page);
            when(patientMapper.selectById("patient-001")).thenReturn(mockPatient);
            when(doctorMapper.selectById(1L)).thenReturn(mockDoctor);

            Result result = reportService.listReports("patient-001", null, 1, 10);

            assertTrue(result.getSuccess());
            assertEquals(1L, result.getTotal());
        }

        @Test
        @DisplayName("按报告类型筛选")
        void listReportsByType() {
            Page<MedicalReport> page = new Page<>(1, 10);
            page.setRecords(List.of());
            page.setTotal(0L);

            when(medicalReportMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(page);

            Result result = reportService.listReports("patient-001", "EXAMINATION", 1, 10);

            assertTrue(result.getSuccess());
            assertEquals(0L, result.getTotal());
        }
    }

    // ==================== 报告详情 ====================

    @Nested
    @DisplayName("报告详情")
    class GetReportDetailTests {

        @Test
        @DisplayName("获取报告详情成功 - 患者本人查看")
        void getDetailSuccess() {
            when(medicalReportMapper.selectById(1L)).thenReturn(mockReport);
            when(patientMapper.selectById("patient-001")).thenReturn(mockPatient);
            when(doctorMapper.selectById(1L)).thenReturn(mockDoctor);

            Result result = reportService.getReportDetail(1L, "patient-001", "patient");

            assertTrue(result.getSuccess());
        }

        @Test
        @DisplayName("获取报告详情 - 管理员可查看")
        void getDetailByAdmin() {
            when(medicalReportMapper.selectById(1L)).thenReturn(mockReport);
            when(patientMapper.selectById("patient-001")).thenReturn(mockPatient);
            when(doctorMapper.selectById(1L)).thenReturn(mockDoctor);

            Result result = reportService.getReportDetail(1L, "other-patient", "admin");

            assertTrue(result.getSuccess());
        }

        @Test
        @DisplayName("报告不存在 - 返回失败")
        void getDetailNotFound() {
            when(medicalReportMapper.selectById(999L)).thenReturn(null);

            Result result = reportService.getReportDetail(999L, "patient-001", "patient");

            assertFalse(result.getSuccess());
            assertEquals("报告不存在", result.getErrorMsg());
        }
    }

    // ==================== 导出PDF ====================

    @Nested
    @DisplayName("导出PDF")
    class ExportPdfTests {

        @Test
        @DisplayName("导出PDF失败 - 报告不存在")
        void exportPdfNotFound() {
            when(medicalReportMapper.selectById(999L)).thenReturn(null);

            String result = reportService.exportPdf(999L);

            assertNull(result);
        }

        @Test
        @DisplayName("导出PDF成功 - 返回文件路径")
        void exportPdfSuccess() {
            when(medicalReportMapper.selectById(1L)).thenReturn(mockReport);
            when(patientMapper.selectById("patient-001")).thenReturn(mockPatient);
            when(medicalReportMapper.updateById(any(MedicalReport.class))).thenReturn(1);

            String result = reportService.exportPdf(1L);

            assertNotNull(result);
            assertTrue(result.endsWith(".pdf"));
        }

        // 注意：PDF生成涉及iText库和文件系统，适合集成测试
        // 此处验证报告不存在的情况和基本PDF生成
    }

    // ==================== 确认报告 ====================

    @Nested
    @DisplayName("确认报告")
    class ConfirmReportTests {

        @Test
        @DisplayName("确认报告成功 - 状态改为已确认")
        void confirmSuccess() {
            when(medicalReportMapper.selectById(1L)).thenReturn(mockReport);
            when(medicalReportMapper.updateById(any(MedicalReport.class))).thenReturn(1);

            Result result = reportService.confirmReport(1L, "patient-001", "patient");

            assertTrue(result.getSuccess());
            verify(medicalReportMapper).updateById(any(MedicalReport.class));
        }

        @Test
        @DisplayName("确认报告失败 - 报告不存在")
        void confirmFailNotFound() {
            when(medicalReportMapper.selectById(999L)).thenReturn(null);

            Result result = reportService.confirmReport(999L, "patient-001", "patient");

            assertFalse(result.getSuccess());
            assertEquals("报告不存在", result.getErrorMsg());
        }

        @Test
        @DisplayName("重复确认 - 仍然成功（幂等操作）")
        void confirmIdempotent() {
            mockReport.setStatus(1); // 已确认
            when(medicalReportMapper.selectById(1L)).thenReturn(mockReport);
            when(medicalReportMapper.updateById(any(MedicalReport.class))).thenReturn(1);

            Result result = reportService.confirmReport(1L, "patient-001", "patient");

            assertTrue(result.getSuccess());
        }
    }
}
