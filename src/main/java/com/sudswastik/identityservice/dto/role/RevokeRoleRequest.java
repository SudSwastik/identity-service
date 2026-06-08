package com.sudswastik.identityservice.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RevokeRoleRequest(
        @NotBlank String cognitoSub,
        @NotNull Long roleId
) {}
