package com.sudswastik.identityservice.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AssignRoleRequest(
        @NotBlank String cognitoSub,
        @NotNull Long roleId
) {}
