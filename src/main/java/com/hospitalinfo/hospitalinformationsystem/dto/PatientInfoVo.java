package com.hospitalinfo.hospitalinformationsystem.dto;

import lombok.Data;

/**
 * 患者信息VO（含就诊统计）
 * 合并原UserDto和PatientInfoVo，统一患者信息展示
 */
@Data
public class PatientInfoVo {
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
    private String avatar;
    private Long totalVisits;
    private Object lastVisitDate;
}
