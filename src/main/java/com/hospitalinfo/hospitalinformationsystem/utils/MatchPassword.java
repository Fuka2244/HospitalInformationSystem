package com.hospitalinfo.hospitalinformationsystem.utils;

import org.springframework.security.crypto.bcrypt.BCrypt;

/**
 * 密码验证工具类
 * 使用BCrypt算法验证密码是否匹配
 */
public class MatchPassword {

    /**
     * 验证密码是否匹配
     * @param plainPassword 明文密码（用户输入的）
     * @param encryptedPassword 加密后的密码（数据库中存储的）
     * @return true-密码匹配，false-密码不匹配
     */
    public static boolean match(String plainPassword, String encryptedPassword) {
        if (plainPassword == null || encryptedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, encryptedPassword);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证密码是否匹配（带空值检查）
     * @param plainPassword 明文密码（用户输入的）
     * @param encryptedPassword 加密后的密码（数据库中存储的）
     * @return true-密码匹配，false-密码不匹配或参数为空
     */
    public static boolean check(String plainPassword, String encryptedPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            return false;
        }
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            return false;
        }
        return match(plainPassword, encryptedPassword);
    }
}
