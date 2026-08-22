package com.cargosetu.repo;

import com.cargosetu.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolesRepo extends JpaRepository<Role,Long> {
    Optional<Role> findByCode(String code);
    Optional<Role> findByCodeAndIsDeletedIsFalse(String roleCode);

    @Query("SELECT COUNT(r) > 0 FROM Role r WHERE r.code = :code AND r.isDeleted = false AND (r.companyId = :companyId OR r.companyId IS NULL)")
    boolean existsByCodeAndCompanyId(@org.springframework.data.repository.query.Param("code") String code, @org.springframework.data.repository.query.Param("companyId") Integer companyId);
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"permissions"})
    @Query("SELECT r FROM Role r WHERE r.isDeleted = false AND (r.companyId = :companyId OR r.companyId IS NULL)")
    List<Role> findAllByIsDeletedIsFalseAndCompanyId(@org.springframework.data.repository.query.Param("companyId") Integer companyId);

    @Query("SELECT r FROM Role r WHERE r.isDeleted = false ORDER BY r.rolesId DESC")
    List<Role> findAllByIsDeletedIsFalseOrderByRolesIdDesc();
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"permissions"})
    @Query("SELECT r FROM Role r WHERE r.isDeleted = false AND r.isShow = true AND (r.companyId = :companyId OR r.companyId IS NULL) ORDER BY r.roleName ASC, r.rolesId DESC")
    List<Role> findAllByIsDeletedIsFalseAndIsShowIsTrueAndCompanyId(@org.springframework.data.repository.query.Param("companyId") Integer companyId);

}
