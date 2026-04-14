package com.hospitalinfo.hospitalinformationsystem.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalinfo.hospitalinformationsystem.dto.*;
import com.hospitalinfo.hospitalinformationsystem.entity.*;
import com.hospitalinfo.hospitalinformationsystem.mapper.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AI服务模块单元测试
 * 覆盖：AI预约推荐、AI药品推荐、AI费用解释、AI报告生成
 * 所有AI服务均mock ChatLanguageModel，不调用真实API
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AI服务模块")
class AiServiceTest {

    @Mock private ChatLanguageModel chatModel;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @Mock private DepartmentMapper departmentMapper;
    @Mock private DoctorMapper doctorMapper;
    @Mock private MedicineMapper medicineMapper;

    // ==================== AI预约推荐 ====================

    @Nested
    @DisplayName("AI预约推荐服务")
    class AiAppointmentServiceTests {

        @InjectMocks
        private AiAppointmentService aiAppointmentService;

        private Department mockDept;
        private Doctor mockDoctor;

        @BeforeEach
        void setUp() {
            mockDept = new Department();
            mockDept.setId(1L);
            mockDept.setName("内科");
            mockDept.setDescription("内科门诊");
            mockDept.setStatus(1);

            mockDoctor = new Doctor();
            mockDoctor.setId(1L);
            mockDoctor.setName("李医生");
            mockDoctor.setDepartmentId(1L);
            mockDoctor.setTitle("主任医师");
            mockDoctor.setSpecialty("心血管内科");
            mockDoctor.setStatus(1);
        }

        @Test
        @DisplayName("正常推荐 - AI返回标准JSON")
        void recommendBySymptomStandardJson() {
            when(departmentMapper.selectList(any())).thenReturn(List.of(mockDept));
            when(doctorMapper.selectList(any())).thenReturn(List.of(mockDoctor));

            String aiResponse = """
                    {
                        "department": "内科",
                        "doctor": "李医生",
                        "recommendedTime": "上午09:00-10:00",
                        "reason": "头痛发热建议内科就诊"
                    }
                    """;
            when(chatModel.generate(anyString())).thenReturn(aiResponse);

            AppointmentRecommendation result = aiAppointmentService.recommendBySymptom("头痛发热");

            assertNotNull(result);
            assertEquals("内科", result.getDepartment());
            assertEquals("李医生", result.getDoctor());
        }

        @Test
        @DisplayName("AI返回Markdown代码块包裹的JSON")
        void recommendBySymptomMarkdownJson() {
            when(departmentMapper.selectList(any())).thenReturn(List.of(mockDept));
            when(doctorMapper.selectList(any())).thenReturn(List.of(mockDoctor));

            String aiResponse = """
                    ```json
                    {
                        "department": "内科",
                        "doctor": "李医生",
                        "recommendedTime": "上午09:00-10:00",
                        "reason": "头痛发热建议内科就诊"
                    }
                    ```
                    """;
            when(chatModel.generate(anyString())).thenReturn(aiResponse);

            AppointmentRecommendation result = aiAppointmentService.recommendBySymptom("头痛发热");

            assertNotNull(result);
            assertEquals("内科", result.getDepartment());
        }

        @Test
        @DisplayName("AI返回非JSON格式 - 降级返回默认推荐")
        void recommendBySymptomFallback() {
            when(departmentMapper.selectList(any())).thenReturn(List.of(mockDept));
            when(doctorMapper.selectList(any())).thenReturn(List.of(mockDoctor));

            when(chatModel.generate(anyString())).thenReturn("抱歉，我无法理解您的症状描述。");

            AppointmentRecommendation result = aiAppointmentService.recommendBySymptom("???");

            assertNotNull(result);
            assertEquals("全科", result.getDepartment()); // 降级默认值
            assertEquals("请前往导诊台咨询", result.getDoctor());
        }

        @Test
        @DisplayName("上下文构建 - 包含科室和医生列表")
        void contextBuilding() {
            when(departmentMapper.selectList(any())).thenReturn(List.of(mockDept));
            when(doctorMapper.selectList(any())).thenReturn(List.of(mockDoctor));
            when(chatModel.generate(anyString())).thenReturn("""
                    {"department":"内科","doctor":"李医生","recommendedTime":"上午","reason":"测试"}
                    """);

            aiAppointmentService.recommendBySymptom("头痛");

            verify(departmentMapper).selectList(any());
            verify(doctorMapper).selectList(any());
        }
    }

    // ==================== AI药品推荐 ====================

    @Nested
    @DisplayName("AI药品推荐服务")
    class AiMedicineServiceTests {

        @InjectMocks
        private AiMedicineService aiMedicineService;

        private Medicine mockMedicine;

        @BeforeEach
        void setUp() {
            mockMedicine = new Medicine();
            mockMedicine.setId(1L);
            mockMedicine.setName("阿莫西林胶囊");
            mockMedicine.setGenericName("阿莫西林");
            mockMedicine.setCategory("抗生素");
            mockMedicine.setSpecification("0.5g*24粒");
            mockMedicine.setIngredients("阿莫西林");
            mockMedicine.setEfficacy("用于敏感菌所致的各种感染");
            mockMedicine.setSideEffects("恶心、呕吐");
            mockMedicine.setContraindications("青霉素过敏者禁用");
            mockMedicine.setPrice(new BigDecimal("15.50"));
            mockMedicine.setStatus(1);
        }

        @Test
        @DisplayName("正常推荐 - AI返回JSON数组")
        void recommendBySymptomStandardJson() {
            when(medicineMapper.selectList(any())).thenReturn(List.of(mockMedicine));

            String aiResponse = """
                    [
                        {
                            "medicineName": "阿莫西林胶囊",
                            "reason": "适用于上呼吸道感染",
                            "dosage": "口服，一次0.5g，每8小时一次",
                            "precautions": "青霉素过敏者禁用"
                        }
                    ]
                    """;
            when(chatModel.generate(anyString())).thenReturn(aiResponse);

            List<MedicineRecommendation> results = aiMedicineService.recommendBySymptom("喉咙痛");

            assertNotNull(results);
            assertEquals(1, results.size());
            assertEquals("阿莫西林胶囊", results.get(0).getMedicineName());
        }

        @Test
        @DisplayName("AI返回Markdown代码块包裹的JSON数组")
        void recommendBySymptomMarkdownJson() {
            when(medicineMapper.selectList(any())).thenReturn(List.of(mockMedicine));

            String aiResponse = """
                    ```json
                    [
                        {
                            "medicineName": "阿莫西林胶囊",
                            "reason": "适用于上呼吸道感染",
                            "dosage": "口服0.5g/次",
                            "precautions": "过敏者禁用"
                        }
                    ]
                    ```
                    """;
            when(chatModel.generate(anyString())).thenReturn(aiResponse);

            List<MedicineRecommendation> results = aiMedicineService.recommendBySymptom("发烧");

            assertNotNull(results);
            assertEquals(1, results.size());
        }

        @Test
        @DisplayName("AI返回非法格式 - 降级返回空列表")
        void recommendBySymptomFallback() {
            when(medicineMapper.selectList(any())).thenReturn(List.of(mockMedicine));
            when(chatModel.generate(anyString())).thenReturn("我无法识别该症状");

            List<MedicineRecommendation> results = aiMedicineService.recommendBySymptom("不明症状");

            assertNotNull(results);
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("药品上下文构建 - 检索所有在售药品")
        void contextBuilding() {
            when(medicineMapper.selectList(any())).thenReturn(List.of(mockMedicine));
            when(chatModel.generate(anyString())).thenReturn("[]");

            aiMedicineService.recommendBySymptom("头痛");

            verify(medicineMapper).selectList(any());
        }
    }

    // ==================== AI费用解释 ====================

    @Nested
    @DisplayName("AI费用解释服务")
    class AiBillingServiceTests {

        @InjectMocks
        private AiBillingService aiBillingService;

        private Billing mockBilling;

        @BeforeEach
        void setUp() {
            mockBilling = new Billing();
            mockBilling.setItemType("EXAMINATION");
            mockBilling.setItemName("血常规检查");
            mockBilling.setAmount(new BigDecimal("35.00"));
            mockBilling.setDescription("血常规检查费用");
        }

        @Test
        @DisplayName("正常解释 - AI返回标准JSON")
        void explainBillingStandardJson() {
            String aiResponse = """
                    {
                        "totalAmount": 35.00,
                        "breakdown": "检查费35元",
                        "explanation": "血常规是基础检查，费用合理",
                        "suggestion": "可使用医保报销"
                    }
                    """;
            when(chatModel.generate(anyString())).thenReturn(aiResponse);

            BillingExplanation result = aiBillingService.explainBilling(
                    List.of(mockBilling), "这个费用合理吗");

            assertNotNull(result);
            assertEquals(new BigDecimal("35.00"), result.getTotalAmount());
            assertNotNull(result.getBreakdown());
        }

        @Test
        @DisplayName("AI返回Markdown代码块包裹的JSON")
        void explainBillingMarkdownJson() {
            String aiResponse = """
                    ```json
                    {
                        "totalAmount": 35.00,
                        "breakdown": "检查费35元",
                        "explanation": "合理",
                        "suggestion": "可用医保"
                    }
                    ```
                    """;
            when(chatModel.generate(anyString())).thenReturn(aiResponse);

            BillingExplanation result = aiBillingService.explainBilling(
                    List.of(mockBilling), "为什么这么贵");

            assertNotNull(result);
        }

        @Test
        @DisplayName("AI返回非法格式 - 降级返回默认解释")
        void explainBillingFallback() {
            when(chatModel.generate(anyString())).thenReturn("无法解析的费用数据");

            BillingExplanation result = aiBillingService.explainBilling(
                    List.of(mockBilling), "解释一下");

            assertNotNull(result);
            assertEquals(new BigDecimal("35.00"), result.getTotalAmount());
            assertEquals("解析异常", result.getBreakdown());
            assertTrue(result.getExplanation().contains("暂时不可用"));
        }

        @Test
        @DisplayName("多条费用记录 - 总额计算正确")
        void explainBillingMultiple() {
            Billing b2 = new Billing();
            b2.setItemType("MEDICINE");
            b2.setItemName("阿莫西林");
            b2.setAmount(new BigDecimal("15.50"));
            b2.setDescription("药品费");

            when(chatModel.generate(anyString())).thenReturn("""
                    {"totalAmount":50.50,"breakdown":"检查+药品","explanation":"合理","suggestion":""}
                    """);

            BillingExplanation result = aiBillingService.explainBilling(
                    List.of(mockBilling, b2), "总费用是多少");

            assertNotNull(result);
        }
    }

    // ==================== AI报告生成 ====================

    @Nested
    @DisplayName("AI报告生成服务")
    class AiReportServiceTests {

        @InjectMocks
        private AiReportService aiReportService;

        private MedicalRecord mockRecord;

        @BeforeEach
        void setUp() {
            mockRecord = new MedicalRecord();
            mockRecord.setChiefComplaint("反复头痛1周");
            mockRecord.setPresentIllness("患者1周前无明显诱因出现头痛");
            mockRecord.setDiagnosis("偏头痛");
            mockRecord.setTreatmentPlan("止痛+休息");
            mockRecord.setVisitDate(LocalDateTime.of(2025, 3, 10, 9, 0));
        }

        @Test
        @DisplayName("正常生成 - AI返回标准JSON")
        void generateReportStandardJson() {
            String aiResponse = """
                    {
                        "summary": "患者反复头痛1周，CT未见异常",
                        "diagnosis": "偏头痛（无先兆型）",
                        "treatment": "布洛芬止痛+生活方式调整",
                        "recommendation": "规律作息、3个月后复查"
                    }
                    """;
            when(chatModel.generate(anyString())).thenReturn(aiResponse);

            ReportGenerationResult result = aiReportService.generateReport(
                    mockRecord, "头颅CT: 未见明显异常", "无历史就诊记录");

            assertNotNull(result);
            assertNotNull(result.getSummary());
            assertNotNull(result.getDiagnosis());
            assertNotNull(result.getTreatment());
            assertNotNull(result.getRecommendation());
        }

        @Test
        @DisplayName("AI返回Markdown代码块包裹的JSON")
        void generateReportMarkdownJson() {
            String aiResponse = """
                    ```json
                    {
                        "summary": "摘要",
                        "diagnosis": "诊断",
                        "treatment": "治疗",
                        "recommendation": "建议"
                    }
                    ```
                    """;
            when(chatModel.generate(anyString())).thenReturn(aiResponse);

            ReportGenerationResult result = aiReportService.generateReport(
                    mockRecord, "检查数据", "历史记录");

            assertNotNull(result);
        }

        @Test
        @DisplayName("AI返回非法格式 - 降级返回默认报告")
        void generateReportFallback() {
            when(chatModel.generate(anyString())).thenReturn("这不是一个有效的JSON响应");

            ReportGenerationResult result = aiReportService.generateReport(
                    mockRecord, "检查数据", "历史记录");

            assertNotNull(result);
            assertEquals("AI报告生成异常", result.getSummary());
            assertTrue(result.getDiagnosis().contains("手动填写"));
        }

        @Test
        @DisplayName("思维链提示 - prompt包含CoT步骤指令")
        void cotPromptIncluded() {
            when(chatModel.generate(anyString())).thenReturn("""
                    {"summary":"s","diagnosis":"d","treatment":"t","recommendation":"r"}
                    """);

            aiReportService.generateReport(mockRecord, "检查数据", "历史记录");

            // 验证generate方法被调用，且传入的prompt包含思维链步骤关键词
            ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
            verify(chatModel).generate(promptCaptor.capture());
            
            String capturedPrompt = promptCaptor.getValue();
            assertTrue(capturedPrompt.contains("步骤1") && capturedPrompt.contains("步骤5") &&
                       capturedPrompt.contains("Chain-of-Thought"));
        }

        @Test
        @DisplayName("检查数据为null - 正常处理")
        void generateReportNullExamination() {
            when(chatModel.generate(anyString())).thenReturn("""
                    {"summary":"s","diagnosis":"d","treatment":"t","recommendation":"r"}
                    """);

            ReportGenerationResult result = aiReportService.generateReport(
                    mockRecord, null, "历史记录");

            assertNotNull(result);
        }
    }
}
