package com.sudswastik.identityservice.service;

import com.sudswastik.identityservice.domain.Role;
import com.sudswastik.identityservice.domain.UserRole;
import com.sudswastik.identityservice.domain.UserRoleId;
import com.sudswastik.identityservice.exception.ConflictException;
import com.sudswastik.identityservice.exception.ResourceNotFoundException;
import com.sudswastik.identityservice.repository.RoleRepository;
import com.sudswastik.identityservice.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class UserRoleService {

    private final UserRoleRepository userRoleRepo;
    private final RoleRepository roleRepo;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Set<Role> getRolesForUser(String cognitoSub) {
        return userRoleRepo.findByIdCognitoSub(cognitoSub).stream()
                .map(UserRole::getRole)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Transactional(readOnly = true)
    public Collection<GrantedAuthority> getAuthoritiesForUser(String cognitoSub) {
        return getRolesForUser(cognitoSub).stream()
                .flatMap(role -> Stream.concat(
                        Stream.of(new SimpleGrantedAuthority("ROLE_" + role.getName())),
                        role.getPermissions().stream()
                                .map(p -> new SimpleGrantedAuthority(p.toPermissionString()))
                ))
                .collect(Collectors.toUnmodifiableSet());
    }

    public void assignRole(String cognitoSub, Long roleId, String actorSub) {
        Role role = roleRepo.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleId));
        UserRoleId id = new UserRoleId(cognitoSub, roleId);
        if (userRoleRepo.existsById(id)) {
            throw new ConflictException("Role '" + role.getName() + "' is already assigned to this user.");
        }
        userRoleRepo.save(UserRole.builder().id(id).role(role).assignedBy(actorSub).build());
        auditLogService.log("UserRole", cognitoSub + ":" + roleId, "ASSIGN", actorSub,
                null, Map.of("cognitoSub", cognitoSub, "roleId", roleId, "roleName", role.getName()));
    }

    public void revokeRole(String cognitoSub, Long roleId, String actorSub) {
        UserRoleId id = new UserRoleId(cognitoSub, roleId);
        UserRole userRole = userRoleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not assigned to this user."));
        auditLogService.log("UserRole", cognitoSub + ":" + roleId, "REVOKE", actorSub,
                Map.of("cognitoSub", cognitoSub, "roleId", roleId,
                        "roleName", userRole.getRole().getName()), null);
        userRoleRepo.delete(userRole);
    }
}
