package com.cargosetu.repo;

import com.cargosetu.model.CompanyAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyAssetRepository extends JpaRepository<CompanyAsset, Long> {
    CompanyAsset findByCompanyIdAndAssetType(Integer companyId, String assetType);
    List<CompanyAsset> findByCompanyId(Integer companyId);
}
