package com.hospitalinfo.hospitalinformationsystem.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 用于配置拦截器
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册登录拦截器
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")                    // 拦截所有路径
                .excludePathPatterns(                      // 排除不需要拦截的路径
                        "/user/login/**",                     // 登录接口及忘记密码接口
                        "/user/register",                  // 注册接口
                        "/medicine/list",                  // 药品列表（公开查询）
                        "/medicine/*",                     // 药品详情（公开查询）
                        "/medicine/ai-recommend",          // AI药品推荐
                        "/appointment/schedules",          // 排班查询（公开）
                        "/appointment/ai-recommend",       // AI预约推荐
                        "/department/**",                  // 科室信息（公开）
                        "/error",                          // 错误页面
                        "/static/**"                       // 静态资源
                );
    }
}
