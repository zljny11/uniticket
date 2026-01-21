package com.uniticket.utils;

import cn.hutool.core.util.StrUtil;
import com.uniticket.dto.AccessTokenClaims;
import com.uniticket.dto.UserDTO;
import com.uniticket.exception.InvalidTokenException;
import com.uniticket.exception.TokenExpiredException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component  // 让Spring管理这个拦截器
public class RefreshTokenInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public RefreshTokenInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1、获取token，并判断token是否存在
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            // token不存在，说明当前用户未登录，不需要刷新直接放行
            return true;
        }
        try {
            AccessTokenClaims claims = jwtUtil.parseAccessToken(token);
            UserDTO userDTO = new UserDTO();
            userDTO.setId(claims.getUserId());
            UserHolder.saveUser(userDTO);
        } catch (TokenExpiredException e) {
            request.setAttribute(AuthConstants.AUTH_ERROR_ATTR, AuthConstants.AUTH_ERROR_EXPIRED);
        } catch (InvalidTokenException e) {
            request.setAttribute(AuthConstants.AUTH_ERROR_ATTR, AuthConstants.AUTH_ERROR_INVALID);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清理ThreadLocal，防止内存泄露
        UserHolder.removeUser();
    }
}
