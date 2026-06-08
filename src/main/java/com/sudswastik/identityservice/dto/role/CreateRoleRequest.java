package com.sudswastik.identityservice.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoleRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 255) String description
) {}
