package com.sudswastik.identityservice.dto.role;

import com.sudswastik.identityservice.domain.Permission;

public record PermissionResponse(Long id, String resource, String action, String permissionString) {

    public static PermissionResponse from(Permission p) {
        return new PermissionResponse(p.getId(), p.getResource(), p.getAction(), p.toPermissionString());
    }
}
