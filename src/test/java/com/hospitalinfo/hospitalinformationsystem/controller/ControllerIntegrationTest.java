package com.hospitalinfo.hospitalinformationsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalinfo.hospitalinformationsystem.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 控制器层集成测试
 * 使用MockMvc模拟HTTP请求，需要启动Spring上下文
 * 注意：运行此测试需要配置test profile的数据库连接
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("控制器集成测试")
class ControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== 患者控制器 ====================

    @Nested
    @DisplayName("患者控制器 - 账户管理")
    class PatientAccountControllerTests {

        @Test
        @DisplayName("注册接口 - 参数校验（空body）")
        void registerWithEmptyBody() throws Exception {
            mockMvc.perform(post("/patient/register")
                            .contentType("application/json")
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("登录接口 - 无效手机号")
        void loginWithInvalidPhone() throws Exception {
            LoginDto dto = new LoginDto();
            dto.setPhone("123");
            dto.setPassword("123456");

            mockMvc.perform(post("/patient/login")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorMsg").value("手机号格式不正确"));
        }

        @Test
        @DisplayName("登出接口 - 未登录状态")
        void logoutWithoutSession() throws Exception {
            mockMvc.perform(post("/patient/loginout"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("患者控制器 - 业务数据")
    class PatientDataControllerTests {

        @Test
        @DisplayName("获取患者信息 - 缺少session中的account")
        void getPatientInfoWithoutSession() throws Exception {
            mockMvc.perform(get("/patient/info"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("查询患者列表 - 无参默认分页")
        void listPatientsDefault() throws Exception {
            MockHttpSession session = new MockHttpSession();
            session.setAttribute("account", "test-account");

            mockMvc.perform(get("/patient/list")
                            .session(session))
                    .andExpect(status().isOk());
        }
    }

    // ==================== 预约控制器 ====================

    @Nested
    @DisplayName("预约控制器")
    class AppointmentControllerTests {

        @Test
        @DisplayName("AI推荐预约 - 公开接口无需登录")
        void aiRecommendWithoutLogin() throws Exception {
            mockMvc.perform(get("/appointment/ai-recommend")
                            .param("symptom", "头痛"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("查询排班 - 公开接口")
        void getSchedulesWithoutLogin() throws Exception {
            mockMvc.perform(get("/appointment/schedules")
                            .param("departmentId", "1"))
                    .andExpect(status().isOk());
        }
    }

    // ==================== 药品控制器 ====================

    @Nested
    @DisplayName("药品控制器")
    class MedicineControllerTests {

        @Test
        @DisplayName("药品列表 - 公开接口")
        void listMedicinesWithoutLogin() throws Exception {
            mockMvc.perform(get("/medicine/list")
                            .param("page", "1")
                            .param("size", "10"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("药品详情 - 不存在的ID")
        void getMedicineDetailNotFound() throws Exception {
            mockMvc.perform(get("/medicine/detail/99999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ==================== 费用控制器 ====================

    @Nested
    @DisplayName("费用控制器")
    class BillingControllerTests {

        @Test
        @DisplayName("费用列表 - 缺少session")
        void listBillingsWithoutSession() throws Exception {
            mockMvc.perform(get("/billing/list")
                            .param("page", "1")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ==================== 报告控制器 ====================

    @Nested
    @DisplayName("报告控制器")
    class MedicalReportControllerTests {

        @Test
        @DisplayName("报告列表 - 缺少session")
        void listReportsWithoutSession() throws Exception {
            mockMvc.perform(get("/report/list")
                            .param("page", "1")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("报告详情 - 不存在的ID")
        void getReportDetailNotFound() throws Exception {
            MockHttpSession session = new MockHttpSession();
            session.setAttribute("account", "test-account");

            mockMvc.perform(get("/report/detail/99999")
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
