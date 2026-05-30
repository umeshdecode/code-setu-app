package com.cargosetu.repo;

import com.cargosetu.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company,Integer> {
    boolean existsByCompanyEmail(String companyEmail);

    Optional<Company> findByCompanyUUIDAndIsDeleted(String companyUUID, Boolean isDeleted);
}
