package com.hospitalinfo.hospitalinformationsystem.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 安全响应头过滤器
 * 为所有HTTP响应添加安全相关的Header，防止常见的Web攻击
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // 防止MIME类型嗅探：浏览器必须使用服务器声明的Content-Type，不允许自行推断
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");

        // 防止点击劫持：禁止页面被嵌入iframe（SAMEORIGIN允许同源嵌入）
        httpResponse.setHeader("X-Frame-Options", "SAMEORIGIN");

        // XSS防护（旧浏览器兼容，已废弃但仍有防御价值）
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");

        // 内容安全策略：限制资源加载来源，防止XSS和数据注入攻击
        String csp = buildCsp(httpRequest);
        httpResponse.setHeader("Content-Security-Policy", csp);

        // 推荐浏览器使用HTTPS（1年有效期，包含子域名）
        httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // 控制Referrer信息泄露：同源请求发送完整Referrer，跨域只发送origin
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // 禁止浏览器缓存敏感页面（可选，对API响应影响较小）
        httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        httpResponse.setHeader("Pragma", "no-cache");

        chain.doFilter(request, response);
    }

    /**
     * 构建CSP策略
     * 开发环境需要宽松一些以支持Vite HMR等功能，生产环境应更严格
     */
    private String buildCsp(HttpServletRequest request) {
        StringBuilder csp = new StringBuilder();

        // 默认策略：只允许同源资源
        csp.append("default-src 'self';");

        // 脚本来源：同源 + 允许eval（Element Plus等组件需要）
        csp.append(" script-src 'self' 'unsafe-eval';");

        // 样式来源：同源 + 内联样式（Element Plus等UI库需要inline style）
        csp.append(" style-src 'self' 'unsafe-inline';");

        // 图片来源：同源 + data URI（头像等base64图片）
        csp.append(" img-src 'self' data: blob:;");

        // 字体来源：同源 + data URI
        csp.append(" font-src 'self' data:;");

        // 连接来源：同源 + localhost（开发环境前后端分离）
        csp.append(" connect-src 'self' http://localhost:* ws://localhost:*;");

        // 框架来源：同源（配合X-Frame-Options）
        csp.append(" frame-src 'self';");

        // 对象/插件来源：禁止
        csp.append(" object-src 'none';");

        // 媒体来源：同源
        csp.append(" media-src 'self';");

        // 表单提交目标：同源
        csp.append(" form-action 'self';");

        // Base URI：同源
        csp.append(" base-uri 'self';");

        return csp.toString();
    }
}
