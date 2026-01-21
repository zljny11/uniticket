package com.uniticket.controller;

import com.uniticket.annotation.Anonymous;
import com.uniticket.dto.LoginFormDTO;
import com.uniticket.dto.RefreshTokenDTO;
import com.uniticket.dto.Result;
import com.uniticket.service.IUserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource
    private IUserService userService;

    @Anonymous
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session) {
        return userService.login(loginForm, session);
    }

    @Anonymous
    @PostMapping("/refresh")
    public Result refresh(@RequestBody RefreshTokenDTO refreshTokenDTO) {
        String token = refreshTokenDTO != null ? refreshTokenDTO.getRefreshToken() : null;
        return userService.refresh(token);
    }
}
