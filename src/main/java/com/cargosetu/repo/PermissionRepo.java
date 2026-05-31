package com.cargosetu.repo;

import com.cargosetu.model.Permission;
import com.cargosetu.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PermissionRepo extends JpaRepository<Permission,Long> {
    boolean existsByCode(String code);
    Optional<Permission> findByCodeAndIsDeletedIsFalse(String permissionCode);

    List<Permission> findAllByIsDeletedIsFalse();
    List<Permission> findAllByIsDeletedIsFalseOrderByIdDesc();

    List<Permission> findAllByCreatedByName(String systemDefault);

}
