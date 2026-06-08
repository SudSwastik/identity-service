package com.sudswastik.identityservice.controller;

import com.sudswastik.identityservice.dto.auth.MessageResponse;
import com.sudswastik.identityservice.dto.role.AssignPermissionsRequest;
import com.sudswastik.identityservice.dto.role.AssignRoleRequest;
import com.sudswastik.identityservice.dto.role.CreateRoleRequest;
import com.sudswastik.identityservice.dto.role.RevokeRoleRequest;
import com.sudswastik.identityservice.dto.role.RoleResponse;
import com.sudswastik.identityservice.dto.role.UpdateRoleRequest;
import com.sudswastik.identityservice.service.RoleService;
import com.sudswastik.identityservice.service.UserRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RolesController {

    private final RoleService roleService;
    private final UserRoleService userRoleService;

    @GetMapping
    public ResponseEntity<List<RoleResponse>> listRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @PostMapping
    public ResponseEntity<RoleResponse> createRole(
            @Valid @RequestBody CreateRoleRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roleService.createRole(request, jwt.getSubject()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleResponse> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(roleService.updateRole(id, request, jwt.getSubject()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        roleService.deleteRole(id, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/permissions")
    public ResponseEntity<RoleResponse> assignPermissions(
            @PathVariable Long id,
            @Valid @RequestBody AssignPermissionsRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(roleService.assignPermissions(id, request.permissionIds(), jwt.getSubject()));
    }

    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    public ResponseEntity<Void> revokePermission(
            @PathVariable Long roleId,
            @PathVariable Long permissionId,
            @AuthenticationPrincipal Jwt jwt) {
        roleService.revokePermission(roleId, permissionId, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/assign")
    public ResponseEntity<MessageResponse> assignRole(
            @Valid @RequestBody AssignRoleRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        userRoleService.assignRole(request.cognitoSub(), request.roleId(), jwt.getSubject());
        return ResponseEntity.ok(new MessageResponse("Role assigned successfully."));
    }

    @DeleteMapping("/revoke")
    public ResponseEntity<MessageResponse> revokeRole(
            @Valid @RequestBody RevokeRoleRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        userRoleService.revokeRole(request.cognitoSub(), request.roleId(), jwt.getSubject());
        return ResponseEntity.ok(new MessageResponse("Role revoked successfully."));
    }
}
