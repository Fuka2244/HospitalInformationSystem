package com.hospitalinfo.hospitalinformationsystem.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalinfo.hospitalinformationsystem.dto.PatientInfoVo;
import com.hospitalinfo.hospitalinformationsystem.service.IRedisSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis会话管理服务实现
 * 使用Redis Hash存储会话信息，Set存储用户的Token列表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSessionServiceImpl implements IRedisSessionService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /** Token会话Key前缀 */
    private static final String TOKEN_SESSION_PREFIX = "jwt:session:";
    /** 用户Token集合Key前缀 */
    private static final String USER_TOKENS_PREFIX = "jwt:user:tokens:";

    /** 默认会话过期时间（秒）：24小时 */
    @Value("${jwt.expiration:86400}")
    private long defaultExpiration;

    @Override
    public void saveSession(String token, PatientInfoVo patientInfo, long expirationSeconds) {
        String sessionKey = TOKEN_SESSION_PREFIX + token;
        String account = patientInfo.getAccount();

        try {
            // 1. 保存会话信息到Hash
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("account", account);
            sessionData.put("phone", patientInfo.getPhone());
            sessionData.put("role", patientInfo.getRole());
            sessionData.put("patientInfo", patientInfo);
            sessionData.put("loginTime", System.currentTimeMillis());

            redisTemplate.opsForHash().putAll(sessionKey, sessionData);
            redisTemplate.expire(sessionKey, expirationSeconds, TimeUnit.SECONDS);

            // 2. 将token添加到用户的Token集合中
            String userTokensKey = USER_TOKENS_PREFIX + account;
            redisTemplate.opsForSet().add(userTokensKey, token);
            redisTemplate.expire(userTokensKey, expirationSeconds, TimeUnit.SECONDS);

            log.info("保存登录会话成功: account={}, expiration={}s", account, expirationSeconds);
        } catch (Exception e) {
            log.error("保存登录会话失败: {}", e.getMessage(), e);
            throw new RuntimeException("保存登录会话失败");
        }
    }

    @Override
    public PatientInfoVo getSession(String token) {
        String sessionKey = TOKEN_SESSION_PREFIX + token;

        try {
            Map<Object, Object> sessionData = redisTemplate.opsForHash().entries(sessionKey);
            if (sessionData == null || sessionData.isEmpty()) {
                return null;
            }

            Object patientInfoObj = sessionData.get("patientInfo");
            if (patientInfoObj == null) {
                return null;
            }

            // 转换为PatientInfoVo
            if (patientInfoObj instanceof PatientInfoVo) {
                return (PatientInfoVo) patientInfoObj;
            }

            return objectMapper.convertValue(patientInfoObj, PatientInfoVo.class);
        } catch (Exception e) {
            log.error("获取登录会话失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void deleteSession(String token) {
        String sessionKey = TOKEN_SESSION_PREFIX + token;

        try {
            // 获取会话信息以便删除用户的Token集合中的token
            Map<Object, Object> sessionData = redisTemplate.opsForHash().entries(sessionKey);
            if (sessionData != null && sessionData.containsKey("account")) {
                String account = (String) sessionData.get("account");
                String userTokensKey = USER_TOKENS_PREFIX + account;
                redisTemplate.opsForSet().remove(userTokensKey, token);
            }

            // 删除会话
            redisTemplate.delete(sessionKey);
            log.info("删除登录会话成功: token={}", token.substring(0, Math.min(10, token.length())) + "...");
        } catch (Exception e) {
            log.error("删除登录会话失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String token) {
        String sessionKey = TOKEN_SESSION_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey));
    }

    @Override
    public void refreshSession(String token, long expirationSeconds) {
        String sessionKey = TOKEN_SESSION_PREFIX + token;

        try {
            // 刷新会话过期时间
            redisTemplate.expire(sessionKey, expirationSeconds, TimeUnit.SECONDS);

            // 获取account，刷新用户的Token集合过期时间
            Object account = redisTemplate.opsForHash().get(sessionKey, "account");
            if (account != null) {
                String userTokensKey = USER_TOKENS_PREFIX + account;
                redisTemplate.expire(userTokensKey, expirationSeconds, TimeUnit.SECONDS);
            }

            log.debug("刷新会话过期时间成功: expiration={}s", expirationSeconds);
        } catch (Exception e) {
            log.error("刷新会话过期时间失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public void logoutByAccount(String account) {
        String userTokensKey = USER_TOKENS_PREFIX + account;

        try {
            // 获取该用户的所有Token
            Set<Object> tokens = redisTemplate.opsForSet().members(userTokensKey);
            if (tokens != null && !tokens.isEmpty()) {
                for (Object token : tokens) {
                    String sessionKey = TOKEN_SESSION_PREFIX + token;
                    redisTemplate.delete(sessionKey);
                }
            }

            // 删除用户的Token集合
            redisTemplate.delete(userTokensKey);
            log.info("踢出用户所有会话成功: account={}", account);
        } catch (Exception e) {
            log.error("踢出用户所有会话失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public List<String> getTokensByAccount(String account) {
        String userTokensKey = USER_TOKENS_PREFIX + account;

        try {
            Set<Object> tokens = redisTemplate.opsForSet().members(userTokensKey);
            if (tokens == null || tokens.isEmpty()) {
                return Collections.emptyList();
            }
            List<String> tokenList = new ArrayList<>();
            for (Object token : tokens) {
                if (token != null) {
                    tokenList.add(token.toString());
                }
            }
            return tokenList;
        } catch (Exception e) {
            log.error("获取用户Token列表失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
