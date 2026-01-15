package com.cargosetu.repo;

import com.cargosetu.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepo extends JpaRepository<Permission,Long> {
}
