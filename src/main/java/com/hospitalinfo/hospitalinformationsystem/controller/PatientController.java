package com.hospitalinfo.hospitalinformationsystem.controller;

import com.hospitalinfo.hospitalinformationsystem.dto.*;
import com.hospitalinfo.hospitalinformationsystem.service.IPatientService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 患者管理控制器
 * 合并原UserController和PatientController，统一管理患者账户和业务数据
 */
@RestController
@RequestMapping("/patient")
@RequiredArgsConstructor
public class PatientController {

    private final IPatientService patientService;

    // ==================== 账户管理 ====================

    /** 登录 */
    @PostMapping("/login")
    public Result login(@RequestBody LoginDto loginDto, HttpSession session) {
        return patientService.login(loginDto, session);
    }

    /** 登出 */
    @PostMapping("/loginout")
    public Result loginOut(HttpSession session) {
        return patientService.loginOut(session);
    }

    /** 注册 */
    @PostMapping("/register")
    public Result register(@RequestBody RegisterDto registerDto) {
        return patientService.register(registerDto);
    }

    /** 查看个人信息 */
    @GetMapping("/me")
    public Result info(HttpSession session) {
        return patientService.info(session);
    }

    /** 修改个人信息 */
    @PutMapping("/me/update")
    public Result update(@RequestBody UpdateDto updateDto, HttpSession session) {
        return patientService.update(updateDto, session);
    }

    /** 发送验证码（忘记密码用） */
    @PostMapping("/login/send-code")
    public Result sendVerificationCode(@RequestBody java.util.Map<String, String> body) {
        String phone = body.get("phone");
        return patientService.sendVerificationCode(phone);
    }

    /** 忘记密码（需验证码） */
    @PutMapping("/login/forget")
    public Result updatePassword(@RequestBody UpdatePasswordDto updatePasswordDto, HttpSession session) {
        return patientService.updatePassword(updatePasswordDto, session);
    }

    /** 验证密码后获取完整身份证号 */
    @GetMapping("/id-card")
    public Result getIdCard(@RequestParam String password, HttpSession session) {
        return patientService.getIdCard(password, session);
    }

    // ==================== 患者业务数据 ====================

    /**
     * 获取当前登录患者的基本信息
     * GET /patient/info
     */
    @GetMapping("/info")
    public Result getMyInfo(HttpSession session) {
        String patientId = (String) session.getAttribute("account");
        return patientService.getPatientInfo(patientId, session);
    }

    /**
     * 根据ID获取患者信息（管理员/医生用）
     * GET /patient/{patientId}
     */
    @GetMapping("/{patientId}")
    public Result getPatientInfo(@PathVariable String patientId, HttpSession session) {
        return patientService.getPatientInfo(patientId, session);
    }

    /**
     * 分页查询患者列表（支持关键字搜索）
     * GET /patient/list?keyword=xxx&page=1&size=10
     */
    @GetMapping("/list")
    public Result listPatients(@RequestParam(required = false) String keyword,
                               @RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer size,
                               HttpSession session) {
        return patientService.listPatients(keyword, page, size, session);
    }

    /**
     * 获取当前患者的电子病历
     * GET /patient/medical-records?page=1&size=10
     */
    @GetMapping("/medical-records")
    public Result getMyMedicalRecords(@RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "10") Integer size,
                                       HttpSession session) {
        String patientId = (String) session.getAttribute("account");
        return patientService.getMedicalRecords(patientId, page, size, session);
    }

    /**
     * 获取指定患者的电子病历（医生/药师/管理员用）
     * GET /patient/{patientId}/medical-records?page=1&size=10
     */
    @GetMapping("/{patientId}/medical-records")
    public Result getPatientMedicalRecords(@PathVariable String patientId,
                                            @RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "10") Integer size,
                                            HttpSession session) {
        return patientService.getMedicalRecords(patientId, page, size, session);
    }

    /**
     * 获取病历详情
     * GET /patient/medical-record/{recordId}
     */
    @GetMapping("/medical-record/{recordId}")
    public Result getMedicalRecordDetail(@PathVariable Long recordId, HttpSession session) {
        return patientService.getMedicalRecordDetail(recordId, session);
    }

    /**
     * 获取当前患者历史就诊记录（支持筛选）
     * GET /patient/visit-history?departmentId=1&doctorId=1&startDate=2024-01-01&endDate=2024-12-31&page=1&size=10
     */
    @GetMapping("/visit-history")
    public Result getMyVisitHistory(@RequestParam(required = false) Long departmentId,
                                     @RequestParam(required = false) Long doctorId,
                                     @RequestParam(required = false) String startDate,
                                     @RequestParam(required = false) String endDate,
                                     @RequestParam(defaultValue = "1") Integer page,
                                     @RequestParam(defaultValue = "10") Integer size,
                                     HttpSession session) {
        String patientId = (String) session.getAttribute("account");
        return patientService.getVisitHistory(patientId, departmentId, doctorId,
                startDate, endDate, page, size, session);
    }
}
