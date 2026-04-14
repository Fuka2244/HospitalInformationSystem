package com.hospitalinfo.hospitalinformationsystem.service;

import com.hospitalinfo.hospitalinformationsystem.utils.EncodePassword;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 患者模块集成测试
 * 合并原UserServiceIntegrationTest
 * 需要启动Spring上下文，连接真实数据库
 *
 * 运行前提：
 * 1. 创建his_test数据库
 * 2. 执行sql/his_schema.sql建表
 * 3. 配置application-test.yaml中的数据库连接
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("患者模块集成测试")
class PatientServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ==================== 登录集成测试 ====================

    @Nested
    @DisplayName("登录流程")
    class LoginIntegrationTests {

        @Test
        @DisplayName("登录失败 - 手机号不存在（完整HTTP流程）")
        void loginFailPhoneNotExist() throws Exception {
            String json = """
                    {"phone":"19900000000","password":"123456"}
                    """;

            mockMvc.perform(post("/HIS/patient/login")
                            .contentType("application/json")
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorMsg").value("手机号不存在"));
        }

        @Test
        @DisplayName("登录失败 - 手机号格式错误（完整HTTP流程）")
        void loginFailInvalidPhone() throws Exception {
            String json = """
                    {"phone":"123","password":"123456"}
                    """;

            mockMvc.perform(post("/HIS/patient/login")
                            .contentType("application/json")
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorMsg").value("手机号格式不正确"));
        }

        @Test
        @DisplayName("注册+登录+登出 完整流程")
        void registerLoginLogout() throws Exception {
            // 1. 注册
            String registerJson = """
                    {
                        "name":"测试用户",
                        "username":"testuser001",
                        "password":"Test@123",
                        "confirmPassword":"Test@123",
                        "gender":"男",
                        "phone":"13700000001",
                        "address":"测试地址",
                        "idCard":"110101200001011234",
                        "age":25
                    }
                    """;

            MvcResult registerResult = mockMvc.perform(post("/HIS/patient/register")
                            .contentType("application/json")
                            .content(registerJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andReturn();

            // 2. 登录
            MockHttpSession session = new MockHttpSession();
            String loginJson = """
                    {"phone":"13700000001","password":"Test@123"}
                    """;

            MvcResult loginResult = mockMvc.perform(post("/HIS/patient/login")
                            .contentType("application/json")
                            .content(loginJson)
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.account").exists())
                    .andReturn();

            // 3. 查询患者信息
            mockMvc.perform(get("/HIS/patient/me")
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // 4. 登出
            mockMvc.perform(post("/HIS/patient/loginout")
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("注册失败 - 两次密码不一致")
        void registerFailPasswordMismatch() throws Exception {
            String registerJson = """
                    {
                        "name":"测试用户",
                        "username":"testuser002",
                        "password":"Test@123",
                        "confirmPassword":"Different@456",
                        "gender":"男",
                        "phone":"13700000002",
                        "address":"测试地址",
                        "idCard":"110101200001012345",
                        "age":25
                    }
                    """;

            mockMvc.perform(post("/HIS/patient/register")
                            .contentType("application/json")
                            .content(registerJson))
                    .andExpect(status().isOk());
        }
    }

    // ==================== 修改密码集成测试 ====================

    @Nested
    @DisplayName("修改密码流程")
    class UpdatePasswordIntegrationTests {

        @Test
        @DisplayName("修改密码 - 完整流程（注册→修改密码→新密码登录）")
        void updatePasswordFullFlow() throws Exception {
            // 1. 注册
            String registerJson = """
                    {
                        "name":"密码测试",
                        "username":"pwdtest",
                        "password":"OldPass@123",
                        "confirmPassword":"OldPass@123",
                        "gender":"女",
                        "phone":"13700000099",
                        "address":"测试",
                        "idCard":"310101200001011234",
                        "age":26
                    }
                    """;

            mockMvc.perform(post("/HIS/patient/register")
                            .contentType("application/json")
                            .content(registerJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // 2. 修改密码
            String updatePwdJson = """
                    {"phone":"13700000099","newPassword":"NewPass@456","confirmPassword":"NewPass@456"}
                    """;

            mockMvc.perform(put("/HIS/patient/login/forget")
                            .contentType("application/json")
                            .content(updatePwdJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // 3. 用新密码登录
            String loginJson = """
                    {"phone":"13700000099","password":"NewPass@456"}
                    """;

            mockMvc.perform(post("/HIS/patient/login")
                            .contentType("application/json")
                            .content(loginJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
}
