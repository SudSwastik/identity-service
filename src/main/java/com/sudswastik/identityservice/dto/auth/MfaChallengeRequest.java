package com.sudswastik.identityservice.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MfaChallengeRequest(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "MFA code must be exactly 6 digits") String mfaCode,
        @NotBlank String session
) {}
