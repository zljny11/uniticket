package com.uniticket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String issuer;
    private String secret;
    private long accessTtlMinutes;
    private long refreshTtlDays;
    private long refreshBufferSeconds;
}
