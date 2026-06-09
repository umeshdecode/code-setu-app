package com.cargosetu.service;

import com.cargosetu.dto.CompanyAssetDTO;
import com.cargosetu.dto.CompanyDTO;
import com.cargosetu.utils.GenericDataDTO;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public interface CompanyService {
    GenericDataDTO createCompany(CompanyDTO company, Long staffId);

    GenericDataDTO updateCompany(Integer id, CompanyDTO company);

    GenericDataDTO getCompanyById(String uuid);

    GenericDataDTO getAllCompanies();

    GenericDataDTO deleteCompany(Integer id);

    void createCompanyAssets(List<CompanyAssetDTO> companyAssets,Integer companyId);

    String getAssetAsBase64(Integer companyId, String assetType);

    GenericDataDTO getCompany(Integer companyId);

    GenericDataDTO getCompanyByStaffId(Long staffId);
}
