package com.cargosetu.repo;

import com.cargosetu.model.CompanyFavouriteTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyFavouriteTemplateRepository extends JpaRepository<CompanyFavouriteTemplate, Long> {

    Optional<CompanyFavouriteTemplate> findByCompanyIdAndTemplateType(Long companyId, String templateType);

    List<CompanyFavouriteTemplate> findByCompanyId(Long companyId);
}
