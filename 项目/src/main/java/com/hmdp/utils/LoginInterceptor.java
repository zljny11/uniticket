package com.hmdp.utils;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoginInterceptor implements HandlerInterceptor {
    /**
     * 前置拦截器，用于判断用户是否登录
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 判断当前用户是否已登录
        if (UserHolder.getUser() == null){
            // 当前用户未登录，直接拦截
            response.setStatus(401);
            return false;
        }
        // 用户存在，直接放行
        return true;
    }
}
