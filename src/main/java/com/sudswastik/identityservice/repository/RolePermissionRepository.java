package com.sudswastik.identityservice.repository;

import com.sudswastik.identityservice.domain.RolePermission;
import com.sudswastik.identityservice.domain.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {
}
