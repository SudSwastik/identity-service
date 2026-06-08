package com.sudswastik.identityservice.dto.role;

import com.sudswastik.identityservice.domain.Role;

import java.util.Set;
import java.util.stream.Collectors;

public record RoleResponse(Long id, String name, String description, Set<PermissionResponse> permissions) {

    public static RoleResponse from(Role r) {
        return new RoleResponse(
                r.getId(),
                r.getName(),
                r.getDescription(),
                r.getPermissions().stream()
                        .map(PermissionResponse::from)
                        .collect(Collectors.toUnmodifiableSet())
        );
    }
}
