package com.cargosetu.repo;

import com.cargosetu.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company,Integer> {
    boolean existsByCompanyEmail(String companyEmail);

    Optional<Company> findByCompanyUUIDAndIsDeleted(String companyUUID, Boolean isDeleted);
    @Query(
            value = "SELECT * FROM cargosetuapp.tblmcompany WHERE created_by = :staffId",
            nativeQuery = true
    )
    Optional<Company> findCompanyByStaffId(@Param("staffId") Long staffId);
}
