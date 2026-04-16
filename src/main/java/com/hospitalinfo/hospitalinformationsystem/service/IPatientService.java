package com.hospitalinfo.hospitalinformationsystem.service;

import com.hospitalinfo.hospitalinformationsystem.dto.*;
import jakarta.servlet.http.HttpSession;

/**
 * 患者服务接口
 * 合并原IUserService和IPatientService，统一管理患者账户信息和业务数据
 */
public interface IPatientService {
    // ========== 账户管理（原IUserService） ==========

    /** 登录 */
    Result login(LoginDto loginDto, HttpSession session);

    /** 注册 */
    Result register(RegisterDto registerDto);

    /** 登出 */
    Result loginOut(HttpSession session);

    /** 查看个人信息（含就诊统计） */
    Result info(HttpSession session);

    /** 修改个人信息 */
    Result update(UpdateDto updateDto, HttpSession session);

    /** 忘记密码 */
    Result updatePassword(UpdatePasswordDto updatePasswordDto, HttpSession session);

    // ========== 患者业务数据（原IPatientService） ==========

    /** 获取患者基本信息 */
    Result getPatientInfo(String patientId, HttpSession session);

    /** 分页查询患者列表 */
    Result listPatients(String keyword, Integer page, Integer size, HttpSession session);

    /** 获取患者电子病历列表 */
    Result getMedicalRecords(String patientId, Integer page, Integer size, HttpSession session);

    /** 获取单条病历详情 */
    Result getMedicalRecordDetail(Long recordId, HttpSession session);

    /** 获取患者历史就诊记录 */
    Result getVisitHistory(String patientId, Long departmentId, Long doctorId,
                           String startDate, String endDate, Integer page, Integer size,
                           HttpSession session);
}
