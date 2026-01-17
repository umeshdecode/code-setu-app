package com.cargosetu.repo;

import com.cargosetu.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RolesRepo extends JpaRepository<Role,Long> {
    Optional<Role> findByCode(String code);
    Optional<Role> findByCodeAndIsDeletedIsFalse(String roleCode);

    boolean existsByCode(String code);
}
