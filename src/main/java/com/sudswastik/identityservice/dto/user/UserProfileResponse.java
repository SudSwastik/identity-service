package com.sudswastik.identityservice.dto.user;

import com.sudswastik.identityservice.dto.role.RoleResponse;

import java.util.Set;

public record UserProfileResponse(
        String sub,
        String email,
        String name,
        boolean emailVerified,
        boolean enabled,
        Set<RoleResponse> roles
) {}
