package com.hospitalinfo.hospitalinformationsystem.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospitalinfo.hospitalinformationsystem.ai.AiBillingService;
import com.hospitalinfo.hospitalinformationsystem.dto.BillingExplanation;
import com.hospitalinfo.hospitalinformationsystem.dto.BillingQueryDto;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.entity.Billing;
import com.hospitalinfo.hospitalinformationsystem.entity.Patient;
import com.hospitalinfo.hospitalinformationsystem.mapper.BillingMapper;
import com.hospitalinfo.hospitalinformationsystem.mapper.PatientMapper;
import com.hospitalinfo.hospitalinformationsystem.service.impl.BillingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 医疗费用查询模块单元测试
 * 覆盖：分页查询费用列表、费用详情、AI费用解释
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("模块4 - 医疗费用查询")
class BillingServiceTest {

    @Mock private BillingMapper billingMapper;
    @Mock private PatientMapper patientMapper;
    @Mock private AiBillingService aiBillingService;

    @InjectMocks
    private BillingServiceImpl billingService;

    private Billing mockBilling;
    private Patient mockPatient;

    @BeforeEach
    void setUp() {
        mockBilling = new Billing();
        mockBilling.setId(1L);
        mockBilling.setPatientId("patient-001");
        mockBilling.setAppointmentId(1L);
        mockBilling.setMedicalRecordId(1L);
        mockBilling.setItemType("EXAMINATION");
        mockBilling.setItemName("血常规检查");
        mockBilling.setAmount(new BigDecimal("35.00"));
        mockBilling.setDescription("血常规检查费用");
        mockBilling.setStatus(1);
        mockBilling.setCreateTime(LocalDateTime.of(2025, 3, 10, 14, 30));

        mockPatient = new Patient();
        mockPatient.setAccount("patient-001");
        mockPatient.setName("张三");
    }

    // ==================== 分页查询费用列表 ====================

    @Nested
    @DisplayName("分页查询费用列表")
    class ListBillingsTests {

        @Test
        @DisplayName("无条件查询 - 返回全部费用")
        void listAllBillings() {
            BillingQueryDto queryDto = new BillingQueryDto();
            queryDto.setPage(1);
            queryDto.setSize(10);

            Page<Billing> page = new Page<>(1, 10);
            page.setRecords(List.of(mockBilling));
            page.setTotal(1L);

            when(billingMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(page);
            when(patientMapper.selectById("patient-001")).thenReturn(mockPatient);

            Result result = billingService.listBillings("patient-001", queryDto);

            assertTrue(result.getSuccess());
            assertEquals(1L, result.getTotal());
        }

        @Test
        @DisplayName("按费用类型筛选 - 检查费")
        void listBillingsByType() {
            BillingQueryDto queryDto = new BillingQueryDto();
            queryDto.setItemType("EXAMINATION");
            queryDto.setPage(1);
            queryDto.setSize(10);

            Page<Billing> page = new Page<>(1, 10);
            page.setRecords(List.of(mockBilling));
            page.setTotal(1L);

            when(billingMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(page);
            when(patientMapper.selectById("patient-001")).thenReturn(mockPatient);

            Result result = billingService.listBillings("patient-001", queryDto);

            assertTrue(result.getSuccess());
        }

        @Test
        @DisplayName("按支付状态筛选 - 已支付")
        void listBillingsByStatus() {
            BillingQueryDto queryDto = new BillingQueryDto();
            queryDto.setStatus(1);
            queryDto.setPage(1);
            queryDto.setSize(10);

            Page<Billing> page = new Page<>(1, 10);
            page.setRecords(List.of());
            page.setTotal(0L);

            when(billingMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(page);

            Result result = billingService.listBillings("patient-001", queryDto);

            assertTrue(result.getSuccess());
            assertEquals(0L, result.getTotal());
        }

        @Test
        @DisplayName("按日期范围筛选")
        void listBillingsByDateRange() {
            BillingQueryDto queryDto = new BillingQueryDto();
            queryDto.setStartDate("2025-01-01");
            queryDto.setEndDate("2025-12-31");
            queryDto.setPage(1);
            queryDto.setSize(10);

            Page<Billing> page = new Page<>(1, 10);
            page.setRecords(List.of(mockBilling));
            page.setTotal(1L);

            when(billingMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(page);
            when(patientMapper.selectById("patient-001")).thenReturn(mockPatient);

            Result result = billingService.listBillings("patient-001", queryDto);

            assertTrue(result.getSuccess());
        }
    }

    // ==================== 费用详情 ====================

    @Nested
    @DisplayName("获取费用详情")
    class GetBillingDetailTests {

        @Test
        @DisplayName("成功获取费用详情 - 填充患者名称")
        void getDetailSuccess() {
            when(billingMapper.selectById(1L)).thenReturn(mockBilling);
            when(patientMapper.selectById("patient-001")).thenReturn(mockPatient);

            Result result = billingService.getBillingDetail(1L, "patient-001", "patient");

            assertTrue(result.getSuccess());
            assertNotNull(result.getData());
        }

        @Test
        @DisplayName("费用记录不存在 - 返回失败")
        void getDetailNotFound() {
            when(billingMapper.selectById(999L)).thenReturn(null);

            Result result = billingService.getBillingDetail(999L, "patient-001", "patient");

            assertFalse(result.getSuccess());
            assertEquals("费用记录不存在", result.getErrorMsg());
        }

        @Test
        @DisplayName("无权查看他人费用 - 返回失败")
        void getDetailNoAccess() {
            when(billingMapper.selectById(1L)).thenReturn(mockBilling);

            Result result = billingService.getBillingDetail(1L, "other-patient", null);

            assertFalse(result.getSuccess());
            assertEquals("无权查看该费用记录", result.getErrorMsg());
        }
    }

    // ==================== AI费用解释 ====================

    @Nested
    @DisplayName("AI费用解释")
    class AiExplainBillingTests {

        @Test
        @DisplayName("AI解释成功 - 返回费用分析")
        void aiExplainSuccess() {
            Billing billing2 = new Billing();
            billing2.setId(2L);
            billing2.setPatientId("patient-001");
            billing2.setItemType("MEDICINE");
            billing2.setItemName("阿莫西林胶囊");
            billing2.setAmount(new BigDecimal("15.50"));
            billing2.setDescription("药品费用");
            billing2.setStatus(1);

            when(billingMapper.selectList(any(QueryWrapper.class)))
                    .thenReturn(List.of(mockBilling, billing2));

            BillingExplanation explanation = new BillingExplanation();
            explanation.setTotalAmount(new BigDecimal("50.50"));
            explanation.setBreakdown("检查费35元 + 药品费15.50元");
            explanation.setExplanation("以上费用均为本次诊疗必要支出");
            explanation.setSuggestion("建议使用医保卡结算可节省部分费用");

            when(aiBillingService.explainBilling(anyList(), eq("这些费用合理吗")))
                    .thenReturn(explanation);

            Result result = billingService.aiExplainBilling(
                    "patient-001", "这些费用合理吗", null, null);

            assertTrue(result.getSuccess());
            assertNotNull(result.getData());
        }

        @Test
        @DisplayName("AI解释 - 指定日期范围")
        void aiExplainWithDateRange() {
            when(billingMapper.selectList(any(QueryWrapper.class)))
                    .thenReturn(List.of(mockBilling));

            BillingExplanation explanation = new BillingExplanation();
            explanation.setTotalAmount(new BigDecimal("35.00"));
            explanation.setBreakdown("检查费35元");
            explanation.setExplanation("费用合理");
            explanation.setSuggestion("");

            when(aiBillingService.explainBilling(anyList(), anyString()))
                    .thenReturn(explanation);

            Result result = billingService.aiExplainBilling(
                    "patient-001", "这个月花了多少", "2025-03-01", "2025-03-31");

            assertTrue(result.getSuccess());
        }

        @Test
        @DisplayName("AI解释失败 - 该时间段无费用记录")
        void aiExplainNoBillings() {
            when(billingMapper.selectList(any(QueryWrapper.class)))
                    .thenReturn(List.of());

            Result result = billingService.aiExplainBilling(
                    "patient-001", "为什么这么贵", "2020-01-01", "2020-12-31");

            assertFalse(result.getSuccess());
            assertEquals("该时间段内无费用记录", result.getErrorMsg());
        }
    }
}
