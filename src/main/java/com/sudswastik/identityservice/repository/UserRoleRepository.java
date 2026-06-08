package com.sudswastik.identityservice.repository;

import com.sudswastik.identityservice.domain.UserRole;
import com.sudswastik.identityservice.domain.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findByIdCognitoSub(String cognitoSub);
}
