package com.hmdp.config;

import com.hmdp.utils.LoginIntercepter;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class MvcConfig implements WebMvcConfigurer {
    
    @Resource
    private LoginIntercepter loginIntercepter;  // 注入Spring管理的拦截器
    
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginIntercepter)  // 使用注入的对象
                .excludePathPatterns(
                        "/user/code",
                        "/user/login",
                        "/blog/hot",
                        "/shop/**",
                        "/shop-type/**",
                        "/upload/**",
                        "/voucher/**"
                );
    }
}
