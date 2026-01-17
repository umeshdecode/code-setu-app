package com.cargosetu.repo;

import com.cargosetu.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepo extends JpaRepository<Department,Integer> {
    boolean existsByCode(String code);
    Optional<Department> findByCodeAndIsDeletedIsFalse(String departmentCode);
}
