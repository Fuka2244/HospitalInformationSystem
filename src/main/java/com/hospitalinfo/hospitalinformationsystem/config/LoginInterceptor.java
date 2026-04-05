package com.hospitalinfo.hospitalinformationsystem.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器
 * 用于验证用户是否已登录
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取Session
        HttpSession session = request.getSession();
        
        // 从Session中获取用户信息
        Object phone = session.getAttribute("phone");
        
        // 判断用户是否已登录
        if (phone == null) {
            // 用户未登录，返回401状态码
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"errorMsg\":\"请先登录\",\"data\":null,\"total\":null}");
            return false;
        }
        
        // 用户已登录，放行
        return true;
    }
}
