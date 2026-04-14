package com.hospitalinfo.hospitalinformationsystem.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospitalinfo.hospitalinformationsystem.dto.*;
import com.hospitalinfo.hospitalinformationsystem.entity.*;
import com.hospitalinfo.hospitalinformationsystem.mapper.*;
import com.hospitalinfo.hospitalinformationsystem.service.impl.PatientServiceImpl;
import com.hospitalinfo.hospitalinformationsystem.utils.EncodePassword;
import com.hospitalinfo.hospitalinformationsystem.utils.MatchPassword;
import com.hospitalinfo.hospitalinformationsystem.utils.RegexTool;
import jakarta.servlet.http.HttpSession;
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
import static org.mockito.Mockito.*;

/**
 * 患者模块单元测试
 * 合并原UserServiceTest和PatientServiceTest
 * 覆盖：登录、注册、登出、信息查询、修改信息、修改密码、
 *       获取患者信息、分页查询患者列表、电子病历列表、病历详情、历史就诊记录
 *
 * 说明：PatientServiceImpl 继承 ServiceImpl，其 lambdaQuery/lambdaUpdate
 * 由 MyBatis-Plus 在运行时提供实现。纯单元测试中无法 mock 这些链式调用，
 * 因此涉及数据库查询的用例仅验证输入校验逻辑，完整流程测试由集成测试覆盖。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("患者模块 - PatientService")
class PatientServiceTest {

    @Mock private PatientMapper patientMapper;
    @Mock private MedicalRecordMapper medicalRecordMapper;
    @Mock private DepartmentMapper departmentMapper;
    @Mock private DoctorMapper doctorMapper;
    @Mock private PrescriptionMapper prescriptionMapper;
    @Mock private PrescriptionItemMapper prescriptionItemMapper;
    @Mock private MedicineMapper medicineMapper;
    @Mock private HttpSession session;

    @InjectMocks
    private PatientServiceImpl patientService;

    private Patient mockPatient;
    private MedicalRecord mockRecord;
    private Department mockDept;
    private Doctor mockDoctor;

    @BeforeEach
    void setUp() {
        mockPatient = new Patient();
        mockPatient.setAccount("test-account-uuid");
        mockPatient.setUsername("testuser");
        mockPatient.setName("张三");
        mockPatient.setPhone("13800138000");
        mockPatient.setPassword(EncodePassword.encrypt("123456"));
        mockPatient.setGender("男");
        mockPatient.setAge(30);
        mockPatient.setIdCard("110101199001011234");
        mockPatient.setAddress("北京市朝阳区");

        mockDept = new Department();
        mockDept.setId(1L);
        mockDept.setName("内科");
        mockDept.setDescription("内科门诊");

        mockDoctor = new Doctor();
        mockDoctor.setId(1L);
        mockDoctor.setName("李医生");
        mockDoctor.setDepartmentId(1L);

        mockRecord = new MedicalRecord();
        mockRecord.setId(1L);
        mockRecord.setPatientId("test-account-uuid");
        mockRecord.setDoctorId(1L);
        mockRecord.setDepartmentId(1L);
        mockRecord.setChiefComplaint("头痛发热");
        mockRecord.setPresentIllness("发热3天，体温38.5℃");
        mockRecord.setDiagnosis("上呼吸道感染");
        mockRecord.setTreatmentPlan("口服退烧药+抗生素");
        mockRecord.setVisitDate(LocalDateTime.of(2025, 1, 15, 9, 0));
        mockRecord.setStatus(1);
    }

    // ==================== 登录测试 ====================

    @Nested
    @DisplayName("登录功能")
    class LoginTests {

        @Test
        @DisplayName("登录失败 - 手机号格式不正确")
        void loginFailInvalidPhone() {
            LoginDto dto = new LoginDto();
            dto.setPhone("123");
            dto.setPassword("123456");

            Result result = patientService.login(dto, session);

            assertFalse(result.getSuccess());
            assertEquals("手机号格式不正确", result.getErrorMsg());
        }

        @Test
        @DisplayName("登录失败 - 空手机号抛异常")
        void loginFailNullPhone() {
            LoginDto dto = new LoginDto();
            dto.setPhone(null);
            dto.setPassword("123456");

            assertThrows(NullPointerException.class, () -> patientService.login(dto, session));
        }
    }

    // ==================== 注册测试 ====================

    @Nested
    @DisplayName("注册功能")
    class RegisterTests {

        @Test
        @DisplayName("注册失败 - 手机号格式不正确")
        void registerFailInvalidPhone() {
            RegisterDto dto = new RegisterDto();
            dto.setPhone("123");
            dto.setIdCard("110101199001011234");
            dto.setPassword("123456");
            dto.setConfirmPassword("123456");

            Result result = patientService.register(dto);
            assertFalse(result.getSuccess());
            assertEquals("手机号或身份证格式不正确", result.getErrorMsg());
        }

        @Test
        @DisplayName("注册失败 - 身份证格式不正确")
        void registerFailInvalidIdCard() {
            RegisterDto dto = new RegisterDto();
            dto.setPhone("13800138000");
            dto.setIdCard("123");
            dto.setPassword("123456");
            dto.setConfirmPassword("123456");

            Result result = patientService.register(dto);
            assertFalse(result.getSuccess());
            assertEquals("手机号或身份证格式不正确", result.getErrorMsg());
        }

        @Test
        @DisplayName("注册失败 - 手机号和身份证都不正确")
        void registerFailBothInvalid() {
            RegisterDto dto = new RegisterDto();
            dto.setPhone("abc");
            dto.setIdCard("xyz");
            dto.setPassword("123456");
            dto.setConfirmPassword("123456");

            Result result = patientService.register(dto);
            assertFalse(result.getSuccess());
            assertEquals("手机号或身份证格式不正确", result.getErrorMsg());
        }
    }

    // ==================== 登出测试 ====================

    @Test
    @DisplayName("登出成功 - 清除session")
    void logoutSuccess() {
        Result result = patientService.loginOut(session);

        assertTrue(result.getSuccess());
        verify(session).removeAttribute("patient");
        verify(session).invalidate();
    }

    // ==================== 查询信息测试 ====================

    @Test
    @DisplayName("查询信息 - 返回session中的患者信息")
    void infoSuccess() {
        PatientInfoVo vo = new PatientInfoVo();
        vo.setUsername("testuser");
        vo.setAccount("test-account-uuid");
        vo.setPhone("13800138000");

        when(session.getAttribute("patient")).thenReturn(vo);
        when(session.getAttribute("account")).thenReturn("test-account-uuid");
        when(patientMapper.selectById("test-account-uuid")).thenReturn(mockPatient);
        when(medicalRecordMapper.selectCount(any(QueryWrapper.class))).thenReturn(5L);
        when(medicalRecordMapper.selectOne(any(QueryWrapper.class))).thenReturn(mockRecord);

        Result result = patientService.info(session);
        assertTrue(result.getSuccess());
        assertNotNull(result.getData());
    }

    @Test
    @DisplayName("查询信息 - session无患者信息返回null")
    void infoNoSession() {
        when(session.getAttribute("patient")).thenReturn(null);

        Result result = patientService.info(session);
        assertTrue(result.getSuccess());
        assertNull(result.getData());
    }

    // ==================== 修改密码测试 ====================

    @Nested
    @DisplayName("修改密码功能")
    class UpdatePasswordTests {

        @Test
        @DisplayName("修改密码失败 - 手机号格式不正确")
        void updatePasswordFailInvalidPhone() {
            UpdatePasswordDto dto = new UpdatePasswordDto();
            dto.setPhone("123");
            dto.setNewPassword("newpass123");
            dto.setConfirmPassword("newpass123");

            Result result = patientService.updatePassword(dto, session);
            assertFalse(result.getSuccess());
            assertEquals("手机号格式不正确", result.getErrorMsg());
        }

        @Test
        @DisplayName("修改密码失败 - 手机号为null")
        void updatePasswordFailNullPhone() {
            UpdatePasswordDto dto = new UpdatePasswordDto();
            dto.setPhone(null);
            dto.setNewPassword("newpass123");
            dto.setConfirmPassword("newpass123");

            Result result = patientService.updatePassword(dto, session);
            assertFalse(result.getSuccess());
            assertEquals("手机号格式不正确", result.getErrorMsg());
        }

        @Test
        @DisplayName("修改密码失败 - 两次密码不一致")
        void updatePasswordFailMismatch() {
            UpdatePasswordDto dto = new UpdatePasswordDto();
            dto.setPhone("13800138000");
            dto.setNewPassword("newpass123");
            dto.setConfirmPassword("different123");

            Result result = patientService.updatePassword(dto, session);
            assertFalse(result.getSuccess());
            assertEquals("两次密码不一致", result.getErrorMsg());
        }

        @Test
        @DisplayName("修改密码失败 - 新密码为null")
        void updatePasswordFailNullNewPassword() {
            UpdatePasswordDto dto = new UpdatePasswordDto();
            dto.setPhone("13800138000");
            dto.setNewPassword(null);
            dto.setConfirmPassword("newpass123");

            Result result = patientService.updatePassword(dto, session);
            assertFalse(result.getSuccess());
            assertEquals("两次密码不一致", result.getErrorMsg());
        }
    }

    // ==================== 工具类测试 ====================

    @Nested
    @DisplayName("密码加密与匹配")
    class PasswordUtilTests {

        @Test
        @DisplayName("BCrypt加密 - 同一明文每次加密结果不同")
        void encryptDifferent() {
            String raw = "123456";
            String hash1 = EncodePassword.encrypt(raw);
            String hash2 = EncodePassword.encrypt(raw);
            assertNotEquals(hash1, hash2);
        }

        @Test
        @DisplayName("BCrypt加密 - 明文与密文匹配")
        void encryptAndMatch() {
            String raw = "mypassword";
            String hash = EncodePassword.encrypt(raw);
            assertTrue(MatchPassword.match(raw, hash));
        }

        @Test
        @DisplayName("BCrypt加密 - 错误密码不匹配")
        void encryptAndNotMatch() {
            String raw = "mypassword";
            String hash = EncodePassword.encrypt(raw);
            assertFalse(MatchPassword.match("wrongpassword", hash));
        }

        @Test
        @DisplayName("BCrypt加密 - 空密码抛异常")
        void encryptNullThrows() {
            assertThrows(IllegalArgumentException.class, () -> EncodePassword.encrypt(null));
            assertThrows(IllegalArgumentException.class, () -> EncodePassword.encrypt(""));
        }

        @Test
        @DisplayName("BCrypt加密 - 强度参数越界抛异常")
        void encryptInvalidStrengthThrows() {
            assertThrows(IllegalArgumentException.class, () -> EncodePassword.encrypt("test", 3));
            assertThrows(IllegalArgumentException.class, () -> EncodePassword.encrypt("test", 32));
        }

        @Test
        @DisplayName("BCrypt加密 - 自定义强度正常工作")
        void encryptWithValidStrength() {
            String hash = EncodePassword.encrypt("test", 4);
            assertTrue(MatchPassword.match("test", hash));
        }
    }

    @Nested
    @DisplayName("正则工具类")
    class RegexToolTests {

        @Test
        @DisplayName("手机号校验 - 合法手机号")
        void validPhone() {
            assertTrue(RegexTool.isPhone("13800138000"));
            assertTrue(RegexTool.isPhone("15012345678"));
            assertTrue(RegexTool.isPhone("19999999999"));
        }

        @Test
        @DisplayName("手机号校验 - 非法手机号")
        void invalidPhone() {
            assertFalse(RegexTool.isPhone("123"));
            assertFalse(RegexTool.isPhone("12345678901"));
            assertFalse(RegexTool.isPhone("23800138000"));
        }

        @Test
        @DisplayName("手机号校验 - 边界值(11位但格式错)")
        void invalidPhoneBoundary() {
            assertFalse(RegexTool.isPhone("10000000000"));
            assertFalse(RegexTool.isPhone("12000000000"));
        }

        @Test
        @DisplayName("身份证校验 - 合法身份证(18位)")
        void validIdCard18() {
            assertTrue(RegexTool.isIdCard("110101199001011234"));
        }

        @Test
        @DisplayName("身份证校验 - 合法身份证(15位)")
        void validIdCard15() {
            assertTrue(RegexTool.isIdCard("110101199001011"));
        }

        @Test
        @DisplayName("身份证校验 - 非法身份证")
        void invalidIdCard() {
            assertFalse(RegexTool.isIdCard("123"));
            assertFalse(RegexTool.isIdCard("11010119900101123"));
            assertFalse(RegexTool.isIdCard("1101011990010112345"));
        }
    }

    // ==================== 获取患者信息 ====================

    @Nested
    @DisplayName("获取患者基本信息")
    class GetPatientInfoTests {

        @Test
        @DisplayName("成功获取患者信息 - 包含就诊统计")
        void getPatientInfoSuccess() {
            when(patientMapper.selectById("test-account-uuid")).thenReturn(mockPatient);
            when(medicalRecordMapper.selectCount(any(QueryWrapper.class))).thenReturn(5L);
            when(medicalRecordMapper.selectOne(any(QueryWrapper.class))).thenReturn(mockRecord);

            Result result = patientService.getPatientInfo("test-account-uuid");

            assertTrue(result.getSuccess());
            assertNotNull(result.getData());
            verify(patientMapper).selectById("test-account-uuid");
            verify(medicalRecordMapper).selectCount(any(QueryWrapper.class));
        }

        @Test
        @DisplayName("患者不存在 - 返回失败")
        void getPatientInfoNotFound() {
            when(patientMapper.selectById("not-exist")).thenReturn(null);

            Result result = patientService.getPatientInfo("not-exist");

            assertFalse(result.getSuccess());
            assertEquals("患者不存在", result.getErrorMsg());
        }

        @Test
        @DisplayName("无就诊记录 - lastVisitDate为空")
        void getPatientInfoNoVisitHistory() {
            when(patientMapper.selectById("test-account-uuid")).thenReturn(mockPatient);
            when(medicalRecordMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
            when(medicalRecordMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

            Result result = patientService.getPatientInfo("test-account-uuid");

            assertTrue(result.getSuccess());
        }
    }

    // ==================== 分页查询患者列表 ====================

    @Nested
    @DisplayName("分页查询患者列表")
    class ListPatientsTests {

        @Test
        @DisplayName("无关键词查询 - 返回全部分页数据")
        void listPatientsNoKeyword() {
            Page<Patient> page = new Page<>(1, 10);
            page.setRecords(List.of(mockPatient));
            page.setTotal(1L);
            when(patientMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(page);

            Result result = patientService.listPatients(null, 1, 10);

            assertTrue(result.getSuccess());
            assertEquals(1L, result.getTotal());
        }

        @Test
        @DisplayName("关键词搜索 - 按姓名/手机/身份证模糊匹配")
        void listPatientsWithKeyword() {
            Page<Patient> page = new Page<>(1, 10);
            page.setRecords(List.of(mockPatient));
            page.setTotal(1L);
            when(patientMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(page);

            Result result = patientService.listPatients("张三", 1, 10);

            assertTrue(result.getSuccess());
            verify(patientMapper).selectPage(any(Page.class), any(QueryWrapper.class));
        }

        @Test
        @DisplayName("空关键词等同于无条件查询")
        void listPatientsEmptyKeyword() {
            Page<Patient> page = new Page<>(1, 10);
            page.setRecords(List.of());
            page.setTotal(0L);
            when(patientMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(page);

            Result result = patientService.listPatients("", 1, 10);

            assertTrue(result.getSuccess());
            assertEquals(0L, result.getTotal());
        }
    }

    // ==================== 电子病历列表 ====================

    @Nested
    @DisplayName("获取电子病历列表")
    class GetMedicalRecordsTests {

        @Test
        @DisplayName("成功获取病历列表 - 填充关联名称")
        void getMedicalRecordsSuccess() {
            Page<MedicalRecord> page = new Page<>(1, 10);
            page.setRecords(List.of(mockRecord));
            page.setTotal(1L);

            when(medicalRecordMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(page);
            when(patientMapper.selectById("test-account-uuid")).thenReturn(mockPatient);
            when(doctorMapper.selectById(1L)).thenReturn(mockDoctor);
            when(departmentMapper.selectById(1L)).thenReturn(mockDept);

            Result result = patientService.getMedicalRecords("test-account-uuid", 1, 10);

            assertTrue(result.getSuccess());
            assertEquals(1L, result.getTotal());
        }

        @Test
        @DisplayName("无病历记录 - 返回空列表")
        void getMedicalRecordsEmpty() {
            Page<MedicalRecord> page = new Page<>(1, 10);
            page.setRecords(List.of());
            page.setTotal(0L);
            when(medicalRecordMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(page);

            Result result = patientService.getMedicalRecords("test-account-uuid", 1, 10);

            assertTrue(result.getSuccess());
            assertEquals(0L, result.getTotal());
        }
    }

    // ==================== 病历详情 ====================

    @Nested
    @DisplayName("获取病历详情")
    class GetMedicalRecordDetailTests {

        @Test
        @DisplayName("成功获取病历详情 - 含处方和药品信息")
        void getDetailWithPrescription() {
            Prescription prescription = new Prescription();
            prescription.setId(1L);
            prescription.setMedicalRecordId(1L);

            PrescriptionItem item = new PrescriptionItem();
            item.setId(1L);
            item.setPrescriptionId(1L);
            item.setMedicineId(1L);

            Medicine medicine = new Medicine();
            medicine.setId(1L);
            medicine.setName("阿莫西林");

            when(medicalRecordMapper.selectById(1L)).thenReturn(mockRecord);
            when(patientMapper.selectById("test-account-uuid")).thenReturn(mockPatient);
            when(doctorMapper.selectById(1L)).thenReturn(mockDoctor);
            when(departmentMapper.selectById(1L)).thenReturn(mockDept);
            when(prescriptionMapper.selectOne(any(QueryWrapper.class))).thenReturn(prescription);
            when(prescriptionItemMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(item));
            when(medicineMapper.selectById(1L)).thenReturn(medicine);

            Result result = patientService.getMedicalRecordDetail(1L);

            assertTrue(result.getSuccess());
            assertNotNull(result.getData());
        }

        @Test
        @DisplayName("病历不存在 - 返回失败")
        void getDetailNotFound() {
            when(medicalRecordMapper.selectById(999L)).thenReturn(null);

            Result result = patientService.getMedicalRecordDetail(999L);

            assertFalse(result.getSuccess());
            assertEquals("病历不存在", result.getErrorMsg());
        }

        @Test
        @DisplayName("病历无处方 - 只返回病历信息")
        void getDetailNoPrescription() {
            when(medicalRecordMapper.selectById(1L)).thenReturn(mockRecord);
            when(patientMapper.selectById("test-account-uuid")).thenReturn(mockPatient);
            when(doctorMapper.selectById(1L)).thenReturn(mockDoctor);
            when(departmentMapper.selectById(1L)).thenReturn(mockDept);
            when(prescriptionMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

            Result result = patientService.getMedicalRecordDetail(1L);

            assertTrue(result.getSuccess());
        }
    }

    // ==================== 历史就诊记录 ====================

    @Nested
    @DisplayName("获取历史就诊记录")
    class GetVisitHistoryTests {

        @Test
        @DisplayName("成功获取就诊历史 - 含处方摘要")
        void getVisitHistorySuccess() {
            Page<MedicalRecord> page = new Page<>(1, 10);
            page.setRecords(List.of(mockRecord));
            page.setTotal(1L);

            Prescription prescription = new Prescription();
            prescription.setId(1L);
            PrescriptionItem item = new PrescriptionItem();
            item.setPrescriptionId(1L);
            item.setMedicineId(1L);
            Medicine med = new Medicine();
            med.setId(1L);
            med.setName("阿莫西林");

            when(medicalRecordMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(page);
            when(patientMapper.selectById("test-account-uuid")).thenReturn(mockPatient);
            when(doctorMapper.selectById(1L)).thenReturn(mockDoctor);
            when(departmentMapper.selectById(1L)).thenReturn(mockDept);
            when(prescriptionMapper.selectOne(any(QueryWrapper.class))).thenReturn(prescription);
            when(prescriptionItemMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(item));
            when(medicineMapper.selectById(1L)).thenReturn(med);

            Result result = patientService.getVisitHistory(
                    "test-account-uuid", null, null, null, null, 1, 10);

            assertTrue(result.getSuccess());
            assertEquals(1L, result.getTotal());
        }

        @Test
        @DisplayName("按科室和日期范围筛选")
        void getVisitHistoryFiltered() {
            Page<MedicalRecord> page = new Page<>(1, 10);
            page.setRecords(List.of());
            page.setTotal(0L);

            when(medicalRecordMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(page);

            Result result = patientService.getVisitHistory(
                    "test-account-uuid", 1L, null, "2025-01-01", "2025-12-31", 1, 10);

            assertTrue(result.getSuccess());
            assertEquals(0L, result.getTotal());
        }
    }
}
