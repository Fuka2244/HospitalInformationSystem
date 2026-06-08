package com.hospitalinfo.hospitalinformationsystem.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 用于配置拦截器和静态资源映射
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Autowired
    private JwtAuthInterceptor jwtAuthInterceptor;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册JWT认证拦截器（优先级高，先执行）
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/**")                    // 拦截所有路径
                .excludePathPatterns(                      // 排除不需要拦截的路径
                        "/patient/login/**",                     // 登录接口及忘记密码接口
                        "/patient/register",                  // 注册接口
                        "/patient/loginout",                  // 登出接口
                        "/staff/login/**",
                        "/staff/loginout/**",
                        "/medicine/list",                  // 药品列表（公开查询）
                        "/medicine/*",                     // 药品详情（公开查询）
                        "/medicine/ai-recommend",          // AI药品推荐
                        "/appointment/schedules",          // 排班查询（公开）
                        "/appointment/ai-recommend",       // AI预约推荐
                        "/appointment/ai-recommend-with-schedules",  // AI预约推荐+可用排班
                        "/department/**",                  // 科室信息（公开）
                        "/uploads/**",                     // 上传文件（公开访问）
                        "/error",                          // 错误页面
                        "/static/**",                      // 静态资源
                        "/actuator/**",                    // Spring Boot Actuator
                        "/swagger-ui/**",                  // Swagger UI
                        "/v3/api-docs/**"                  // OpenAPI文档
                )
                .order(1);  // 设置优先级

        // 注册旧版Session登录拦截器（保留兼容，作为备选）
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/patient/login/**",
                        "/patient/register",
                        "/patient/loginout",
                        "/staff/login/**",
                        "/staff/loginout/**",
                        "/medicine/list",
                        "/medicine/*",
                        "/medicine/ai-recommend",
                        "/appointment/schedules",
                        "/appointment/ai-recommend",
                        "/appointment/ai-recommend-with-schedules",
                        "/department/**",
                        "/uploads/**",
                        "/error",
                        "/static/**"
                )
                .order(2);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射上传文件目录，使头像等上传文件可以通过URL直接访问
        String absolutePath = new java.io.File(uploadDir).getAbsolutePath();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolutePath + "/");
    }
}
