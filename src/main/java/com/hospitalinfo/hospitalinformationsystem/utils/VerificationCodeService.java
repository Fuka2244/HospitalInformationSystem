package com.hospitalinfo.hospitalinformationsystem.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务（基于Redis存储）
 * 提供验证码的生成、存储、校验功能
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationCodeService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final int CODE_LENGTH = 6;
    private static final long CODE_EXPIRE_MINUTES = 5;       // 验证码5分钟过期
    private static final long CODE_INTERVAL_SECONDS = 60;    // 60秒发送间隔

    /** Redis Key前缀 */
    private static final String CODE_KEY_PREFIX = "verify:code:";
    /** 发送间隔Key前缀 */
    private static final String INTERVAL_KEY_PREFIX = "verify:interval:";

    /**
     * 生成验证码并存储到Redis
     * @param phone 手机号
     * @return 验证码（生产环境不应返回，应通过短信发送）；如果发送过于频繁返回null
     */
    public String generateCode(String phone) {
        String intervalKey = INTERVAL_KEY_PREFIX + phone;

        // 检查发送间隔（如果间隔Key存在，说明发送过于频繁）
        if (Boolean.TRUE.equals(redisTemplate.hasKey(intervalKey))) {
            return null;
        }

        String code = generateRandomCode();

        // 存储验证码到Redis，5分钟过期
        String codeKey = CODE_KEY_PREFIX + phone;
        redisTemplate.opsForValue().set(codeKey, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        // 设置发送间隔标记，60秒后自动过期
        redisTemplate.opsForValue().set(intervalKey, "1", CODE_INTERVAL_SECONDS, TimeUnit.SECONDS);

        log.info("验证码已生成(Redis) - 手机号: {}, 验证码: {}", phone, code);
        return code;
    }

    /**
     * 校验验证码
     * @param phone 手机号
     * @param code 验证码
     * @return 是否正确
     */
    public boolean verifyCode(String phone, String code) {
        if (phone == null || code == null) {
            return false;
        }

        String codeKey = CODE_KEY_PREFIX + phone;
        Object storedCode = redisTemplate.opsForValue().get(codeKey);

        if (storedCode == null) {
            return false;
        }

        // 验证码正确后删除，防止重复使用
        if (code.equals(storedCode.toString())) {
            redisTemplate.delete(codeKey);
            return true;
        }

        return false;
    }

    private String generateRandomCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
