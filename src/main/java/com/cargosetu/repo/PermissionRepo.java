package com.cargosetu.repo;

import com.cargosetu.model.Permission;
import com.cargosetu.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepo extends JpaRepository<Permission,Long> {
    boolean existsByCode(String code);
    Optional<Permission> findByCodeAndIsDeletedIsFalse(String permissionCode);
}
