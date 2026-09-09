package com.uniticket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenPairDTO {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
}
