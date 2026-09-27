package com.cargosetu.repo;

import com.cargosetu.model.Staff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {

    Optional<Staff> findByUserName(String superAdmin);

    Optional<Staff> findByEmail(String email);
    Optional<Staff> findByEmailOrUserName(String email,String username);
    Optional<Staff> findByMobileNumberAndIsDeleted(String mobileNumber, boolean isDeleted);
    Optional<Staff> findByStaffIdAndIsDeleted(Long staffId, boolean isDeleted);
    Page<Staff> findAllByCompanyIdOrderByStaffIdDesc(Integer companyId, Pageable pageable);

    /**
     * Active, non-deleted Admin staff (SUPERADMIN/COMPANY_ADMIN) scoped to a single company.
     * Used by TripOpenedEventListener to resolve who should receive the "new load available"
     * email; email format/blank filtering happens afterwards in-memory since Staff.email has
     * no DB-level validation.
     */
    @Query("SELECT DISTINCT s FROM Staff s JOIN s.roles r WHERE s.companyId = :companyId " +
            "AND s.isActive = true AND s.isDeleted = false AND r.code IN :roleCodes")
    List<Staff> findActiveAdminsByCompanyIdAndRoleCodes(@Param("companyId") Integer companyId,
                                                         @Param("roleCodes") List<String> roleCodes);
}
