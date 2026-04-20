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

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册登录拦截器
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")                    // 拦截所有路径
                .excludePathPatterns(                      // 排除不需要拦截的路径
                        "/patient/login/**",                     // 登录接口及忘记密码接口
                        "/patient/register",                  // 注册接口
                        "/medicine/list",                  // 药品列表（公开查询）
                        "/medicine/*",                     // 药品详情（公开查询）
                        "/medicine/ai-recommend",          // AI药品推荐
                        "/appointment/schedules",          // 排班查询（公开）
                        "/appointment/ai-recommend",       // AI预约推荐
                        "/appointment/ai-recommend-with-schedules",  // AI预约推荐+可用排班
                        "/department/**",                  // 科室信息（公开）
                        "/uploads/**",                     // 上传文件（公开访问）
                        "/error",                          // 错误页面
                        "/static/**"                       // 静态资源
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射上传文件目录，使头像等上传文件可以通过URL直接访问
        String absolutePath = new java.io.File(uploadDir).getAbsolutePath();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolutePath + "/");
    }
}
