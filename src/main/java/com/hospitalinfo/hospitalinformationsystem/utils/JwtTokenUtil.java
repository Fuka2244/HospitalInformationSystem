package com.hospitalinfo.hospitalinformationsystem.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token 工具类
 * 用于生成和验证 JWT Token
 */
@Slf4j
@Component
public class JwtTokenUtil {

    @Value("${jwt.secret:HisSecretKeyForJwtToken2024MustBeAtLeast256Bits}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration; // 默认24小时

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration; // 默认7天

    /**
     * 生成Token
     */
    public String generateToken(String account, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("account", account);
        claims.put("role", role);
        return createToken(claims, account);
    }

    /**
     * 生成Token（带额外信息）
     */
    public String generateToken(String account, String role, Map<String, Object> extraClaims) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("account", account);
        claims.put("role", role);
        if (extraClaims != null) {
            claims.putAll(extraClaims);
        }
        return createToken(claims, account);
    }

    /**
     * 生成刷新Token
     */
    public String generateRefreshToken(String account) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("account", account);
        claims.put("type", "refresh");
        return createToken(claims, account, refreshExpiration);
    }

    /**
     * 创建Token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return createToken(claims, subject, expiration);
    }

    private String createToken(Map<String, Object> claims, String subject, long expirationMs) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 获取签名Key
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 从Token中提取account
     */
    public String extractAccount(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 从Token中提取role
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * 从Token中提取过期时间
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 提取单个claim
     */
    public <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 提取所有claims
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 检查Token是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * 验证Token
     */
    public boolean validateToken(String token, String account) {
        try {
            final String tokenAccount = extractAccount(token);
            return (tokenAccount.equals(account) && !isTokenExpired(token));
        } catch (JwtException e) {
            log.warn("JWT验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证Token（不验证账户）
     */
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (JwtException e) {
            log.warn("JWT验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取Token剩余有效期（秒）
     */
    public long getRemainingTime(String token) {
        try {
            Date expiration = extractExpiration(token);
            long remaining = (expiration.getTime() - System.currentTimeMillis()) / 1000;
            return Math.max(0, remaining);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 获取Token剩余有效期（毫秒）
     */
    public long getRemainingTimeMillis(String token) {
        try {
            Date expiration = extractExpiration(token);
            long remaining = expiration.getTime() - System.currentTimeMillis();
            return Math.max(0, remaining);
        } catch (Exception e) {
            return 0;
        }
    }
}
