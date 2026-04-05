package com.hospitalinfo.hospitalinformationsystem.utils;

import org.springframework.security.crypto.bcrypt.BCrypt;

/**
 * 密码加密工具类
 * 使用BCrypt算法对密码进行加密
 */
public class EncodePassword {

    /**
     * 对密码进行加密
     * @param password 明文密码
     * @return 加密后的密码
     */
    public static String encrypt(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        // BCrypt.hashpw会自动生成随机盐
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    /**
     * 对密码进行加密，可指定加密强度
     * @param password 明文密码
     * @param strength 加密强度（4-31），默认为10
     * @return 加密后的密码
     */
    public static String encrypt(String password, int strength) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (strength < 4 || strength > 31) {
            throw new IllegalArgumentException("加密强度必须在4-31之间");
        }
        return BCrypt.hashpw(password, BCrypt.gensalt(strength));
    }
}
