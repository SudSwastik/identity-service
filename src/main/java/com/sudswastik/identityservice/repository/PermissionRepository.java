package com.sudswastik.identityservice.repository;

import com.sudswastik.identityservice.domain.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByResourceAndAction(String resource, String action);

    boolean existsByResourceAndAction(String resource, String action);
}
