package com.hospitalinfo.hospitalinformationsystem.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 患者信息VO（含就诊统计）
 * 合并原UserDto和PatientInfoVo，统一患者信息展示
 */
@Data
public class PatientInfoVo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String account;
    private String username;
    private String name;
    private String gender;
    private Integer age;
    private String phone;
    private String address;
    private String idCard;
    /** 是否已验证身份（身份证是否脱敏） */
    private boolean idCardVerified;
    /** 手机号是否脱敏 */
    private boolean phoneMasked;
    private String avatar;
    private Long totalVisits;
    private Object lastVisitDate;
    /** 用户角色：patient, doctor, pharmacist, admin */
    private String role;
}
