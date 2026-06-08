package com.hospitalinfo.hospitalinformationsystem.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalinfo.hospitalinformationsystem.dto.PatientInfoVo;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.exception.ErrorCode;
import com.hospitalinfo.hospitalinformationsystem.service.IRedisSessionService;
import com.hospitalinfo.hospitalinformationsystem.utils.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * JWT认证拦截器
 * 验证请求头中的JWT Token，并从Redis中恢复会话状态
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtTokenUtil jwtTokenUtil;
    private final IRedisSessionService redisSessionService;
    private final ObjectMapper objectMapper;

    /** 请求头中的Token名称 */
    @Value("${jwt.header:Authorization}")
    private String tokenHeader;

    /** Token前缀 */
    @Value("${jwt.token-prefix:Bearer }")
    private String tokenPrefix;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取Token
        String token = extractToken(request);

        if (token == null) {
            sendUnauthorizedResponse(request, response, "未提供认证令牌");
            return false;
        }

        // 验证Token格式和有效期
        if (!jwtTokenUtil.validateToken(token)) {
            sendUnauthorizedResponse(request, response, "认证令牌无效或已过期");
            return false;
        }

        // 从Redis中获取会话信息
        PatientInfoVo patientInfo = redisSessionService.getSession(token);
        if (patientInfo == null) {
            sendUnauthorizedResponse(request, response, "会话已失效，请重新登录");
            return false;
        }

        // 将用户信息存入请求属性，供Controller使用
        request.setAttribute("patientInfo", patientInfo);
        request.setAttribute("account", patientInfo.getAccount());
        request.setAttribute("role", patientInfo.getRole());
        request.getSession().setAttribute("patient", patientInfo);
        request.getSession().setAttribute("account", patientInfo.getAccount());
        request.getSession().setAttribute("role", patientInfo.getRole());
        request.getSession().setAttribute("phone", patientInfo.getPhone());
        setRoleSpecificSessionAttributes(request, patientInfo);

        // 刷新会话过期时间（可选，用于保持活跃）
        long remainingTime = jwtTokenUtil.getRemainingTime(token);
        if (remainingTime > 300) { // 剩余时间大于5分钟才刷新
            redisSessionService.refreshSession(token, remainingTime);
        }

        log.debug("JWT认证成功: account={}", patientInfo.getAccount());
        return true;
    }

    /**
     * 从请求中提取Token
     */
    private String extractToken(HttpServletRequest request) {
        // 优先从请求头获取
        String bearerToken = request.getHeader(tokenHeader);
        if (StringUtils.hasText(bearerToken)) {
            if (bearerToken.startsWith(tokenPrefix)) {
                return bearerToken.substring(tokenPrefix.length());
            }
            return bearerToken;
        }

        // 备用：从请求参数获取（用于文件上传等场景）
        String paramToken = request.getParameter("token");
        if (StringUtils.hasText(paramToken)) {
            return paramToken;
        }

        return null;
    }

    private void setRoleSpecificSessionAttributes(HttpServletRequest request, PatientInfoVo patientInfo) {
        try {
            Long staffId = Long.valueOf(patientInfo.getAccount());
            if ("doctor".equals(patientInfo.getRole())) {
                request.getSession().setAttribute("doctorId", staffId);
            } else if ("pharmacist".equals(patientInfo.getRole())) {
                request.getSession().setAttribute("pharmacistId", staffId);
            } else if ("admin".equals(patientInfo.getRole())) {
                request.getSession().setAttribute("adminId", staffId);
            }
        } catch (NumberFormatException ignored) {
            // Patient accounts are UUID strings and do not need numeric staff IDs.
        }
    }

    /**
     * 发送未授权响应
     */
    private void sendUnauthorizedResponse(HttpServletRequest request, HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Result<?> result = Result.fail(ErrorCode.UNAUTHORIZED, message);
        result.setPath(request.getRequestURI());
        String traceId = request.getHeader("X-Trace-Id");
        result.setTraceId(StringUtils.hasText(traceId) ? traceId : UUID.randomUUID().toString());
        response.getWriter().write(objectMapper.writeValueAsString(result));
        log.debug("JWT认证失败: {}", message);
    }
}
