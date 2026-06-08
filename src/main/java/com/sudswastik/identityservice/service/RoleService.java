package com.sudswastik.identityservice.service;

import com.sudswastik.identityservice.domain.Permission;
import com.sudswastik.identityservice.domain.Role;
import com.sudswastik.identityservice.domain.RolePermission;
import com.sudswastik.identityservice.domain.RolePermissionId;
import com.sudswastik.identityservice.dto.role.CreateRoleRequest;
import com.sudswastik.identityservice.dto.role.RoleResponse;
import com.sudswastik.identityservice.dto.role.UpdateRoleRequest;
import com.sudswastik.identityservice.exception.ConflictException;
import com.sudswastik.identityservice.exception.ResourceNotFoundException;
import com.sudswastik.identityservice.repository.PermissionRepository;
import com.sudswastik.identityservice.repository.RolePermissionRepository;
import com.sudswastik.identityservice.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleService {

    private final RoleRepository roleRepo;
    private final PermissionRepository permRepo;
    private final RolePermissionRepository rolePermRepo;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepo.findAll().stream().map(RoleResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RoleResponse getRoleById(Long id) {
        return RoleResponse.from(findOrThrow(id));
    }

    public RoleResponse createRole(CreateRoleRequest request, String actorSub) {
        if (roleRepo.existsByName(request.name())) {
            throw new ConflictException("Role '" + request.name() + "' already exists.");
        }
        Role role = Role.builder()
                .name(request.name())
                .description(request.description())
                .build();
        role = roleRepo.save(role);
        auditLogService.log("Role", String.valueOf(role.getId()), "CREATE", actorSub,
                null, Map.of("id", role.getId(), "name", role.getName()));
        return RoleResponse.from(role);
    }

    public RoleResponse updateRole(Long id, UpdateRoleRequest request, String actorSub) {
        Role role = findOrThrow(id);
        if (!role.getName().equals(request.name()) && roleRepo.existsByName(request.name())) {
            throw new ConflictException("Role '" + request.name() + "' already exists.");
        }
        Map<String, Object> oldSnapshot = snapshot(role);
        role.setName(request.name());
        role.setDescription(request.description());
        role = roleRepo.save(role);
        auditLogService.log("Role", String.valueOf(id), "UPDATE", actorSub, oldSnapshot, snapshot(role));
        return RoleResponse.from(role);
    }

    public void deleteRole(Long id, String actorSub) {
        Role role = findOrThrow(id);
        auditLogService.log("Role", String.valueOf(id), "DELETE", actorSub, snapshot(role), null);
        roleRepo.delete(role);
    }

    public RoleResponse assignPermissions(Long roleId, List<Long> permissionIds, String actorSub) {
        Role role = findOrThrow(roleId);
        for (Long permId : permissionIds) {
            Permission perm = permRepo.findById(permId)
                    .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permId));
            RolePermissionId rpId = new RolePermissionId(roleId, permId);
            if (!rolePermRepo.existsById(rpId)) {
                RolePermission rp = RolePermission.builder()
                        .id(rpId).role(role).permission(perm).assignedBy(actorSub).build();
                rolePermRepo.save(rp);
                auditLogService.log("RolePermission", roleId + ":" + permId, "ASSIGN", actorSub,
                        null, Map.of("roleId", roleId, "permissionId", permId,
                                "permission", perm.toPermissionString()));
            }
        }
        return RoleResponse.from(roleRepo.findById(roleId).orElseThrow());
    }

    public void revokePermission(Long roleId, Long permissionId, String actorSub) {
        RolePermissionId rpId = new RolePermissionId(roleId, permissionId);
        RolePermission rp = rolePermRepo.findById(rpId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not assigned to this role."));
        auditLogService.log("RolePermission", roleId + ":" + permissionId, "REVOKE", actorSub,
                Map.of("roleId", roleId, "permissionId", permissionId,
                        "permission", rp.getPermission().toPermissionString()), null);
        rolePermRepo.delete(rp);
    }

    private Role findOrThrow(Long id) {
        return roleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));
    }

    private Map<String, Object> snapshot(Role role) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", role.getId());
        map.put("name", role.getName());
        map.put("description", role.getDescription());
        return map;
    }
}
