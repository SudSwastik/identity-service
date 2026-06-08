package com.sudswastik.identityservice.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Email String email,
        @NotBlank String confirmationCode,
        @NotBlank @Size(min = 8) String newPassword
) {}
