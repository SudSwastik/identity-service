package com.sudswastik.identityservice.dto.auth;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private String accessToken;
    private String idToken;
    private String refreshToken;
    private Integer expiresIn;

    // Populated only when a challenge (e.g. MFA) is required instead of tokens
    private String challengeName;
    private String session;
}
