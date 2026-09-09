package com.uniticket.utils;

import cn.hutool.json.JSONUtil;
import com.uniticket.annotation.Anonymous;
import com.uniticket.dto.Result;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

@Component
public class LoginCheckInterceptor implements HandlerInterceptor {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final List<String> whiteList = Arrays.asList(
            "/user/code",
            "/user/login",
            "/auth/login",
            "/auth/refresh",
            "/campus-post/**",
            "/blog/hot",
            "/venue/**",
            "/venue-category/**",
            "/ticket/**",
            "/upload/**",
            "/voucher/**"
    );

    /**
     * 前置拦截器，用于判断用户是否登录
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            if (handlerMethod.hasMethodAnnotation(Anonymous.class)
                    || handlerMethod.getBeanType().isAnnotationPresent(Anonymous.class)) {
                return true;
            }
        }
        String path = request.getRequestURI();
        for (String pattern : whiteList) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        if (UserHolder.getUser() == null) {
            String error = (String) request.getAttribute(AuthConstants.AUTH_ERROR_ATTR);
            String message;
            if (AuthConstants.AUTH_ERROR_EXPIRED.equals(error)) {
                message = "TokenExpired";
            } else if (AuthConstants.AUTH_ERROR_INVALID.equals(error)) {
                message = "InvalidToken";
            } else {
                message = "Unauthorized";
            }
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSONUtil.toJsonStr(Result.fail(message)));
            return false;
        }
        return true;
    }
}
