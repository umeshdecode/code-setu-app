package com.cargosetu.repo;

import com.cargosetu.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepository extends JpaRepository<Company,Integer> {
    boolean existsByCompanyEmail(String companyEmail);

}
