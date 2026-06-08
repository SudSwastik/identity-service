package com.sudswastik.identityservice.dto.mfa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyMfaSetupRequest(
        @NotBlank @Pattern(regexp = "\\d{6}", message = "TOTP code must be exactly 6 digits") String userCode
) {}
