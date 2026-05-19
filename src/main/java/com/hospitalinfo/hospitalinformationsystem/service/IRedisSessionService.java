package com.hospitalinfo.hospitalinformationsystem.service;

import com.hospitalinfo.hospitalinformationsystem.dto.PatientInfoVo;

/**
 * Redis会话管理服务接口
 * 管理登录状态和Token
 */
public interface IRedisSessionService {

    /**
     * 保存登录会话
     * @param token JWT Token
     * @param patientInfo 患者信息
     * @param expirationSeconds 过期时间（秒）
     */
    void saveSession(String token, PatientInfoVo patientInfo, long expirationSeconds);

    /**
     * 根据Token获取登录会话
     * @param token JWT Token
     * @return 患者信息，不存在返回null
     */
    PatientInfoVo getSession(String token);

    /**
     * 删除登录会话
     * @param token JWT Token
     */
    void deleteSession(String token);

    /**
     * 检查Token是否存在
     * @param token JWT Token
     * @return 是否存在
     */
    boolean exists(String token);

    /**
     * 刷新会话过期时间
     * @param token JWT Token
     * @param expirationSeconds 新过期时间（秒）
     */
    void refreshSession(String token, long expirationSeconds);

    /**
     * 根据account踢出所有登录会话
     * @param account 账户
     */
    void logoutByAccount(String account);

    /**
     * 获取account对应的所有Token
     * @param account 账户
     * @return Token列表
     */
    java.util.List<String> getTokensByAccount(String account);
}
