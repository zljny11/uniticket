package com.uniticket.utils;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.uniticket.config.JwtProperties;
import com.uniticket.dto.AccessTokenClaims;
import com.uniticket.exception.InvalidTokenException;
import com.uniticket.exception.TokenExpiredException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private final JwtProperties properties;
    private final JWTSigner signer;

    public JwtUtil(JwtProperties properties) {
        this.properties = properties;
        this.signer = JWTSignerUtil.createSigner("HS256", properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId, String scopes) {
        long now = System.currentTimeMillis();
        long expMs = now + properties.getAccessTtlMinutes() * 60L * 1000L;
        Map<String, Object> payload = new HashMap<>();
        payload.put("iss", properties.getIssuer());
        payload.put("iat", new Date(now));
        payload.put("exp", new Date(expMs));
        payload.put("userId", userId);
        payload.put("scopes", scopes);
        return JWTUtil.createToken(payload, signer);
    }

    public AccessTokenClaims parseAccessToken(String token) {
        JWT jwt;
        try {
            jwt = JWTUtil.parseToken(token);
        } catch (Exception e) {
            throw new InvalidTokenException("Invalid access token");
        }
        if (!JWTUtil.verify(token, signer)) {
            throw new InvalidTokenException("Invalid access token");
        }
        try {
            JWTValidator.of(jwt).validateDate();
        } catch (Exception e) {
            throw new TokenExpiredException("Access token expired");
        }
        Object issuerValue = jwt.getPayload("iss");
        if (issuerValue == null || !properties.getIssuer().equals(String.valueOf(issuerValue))) {
            throw new InvalidTokenException("Invalid access token");
        }
        Object userIdValue = jwt.getPayload("userId");
        Object scopesValue = jwt.getPayload("scopes");
        if (userIdValue == null) {
            throw new InvalidTokenException("Invalid access token");
        }
        Long userId;
        try {
            userId = Long.valueOf(String.valueOf(userIdValue));
        } catch (NumberFormatException e) {
            throw new InvalidTokenException("Invalid access token");
        }
        String scopes = scopesValue == null ? "" : String.valueOf(scopesValue);
        return new AccessTokenClaims(userId, scopes);
    }
}
