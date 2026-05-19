package com.hospitalinfo.hospitalinformationsystem.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 登录响应DTO
 * 包含JWT Token信息
 */
@Data
@Accessors(chain = true)
public class LoginResponseDto {
    /** JWT访问令牌 */
    private String token;
    /** 刷新令牌 */
    private String refreshToken;
    /** 令牌类型 */
    private String tokenType = "Bearer";
    /** 过期时间（秒） */
    private Long expiresIn;
    /** 患者信息 */
    private PatientInfoVo patientInfo;
}
