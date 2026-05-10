package com.cargosetu.service;

import com.cargosetu.dto.CompanyAssetDTO;
import com.cargosetu.dto.CompanyDTO;
import com.cargosetu.model.Company;
import com.cargosetu.utils.GenericDataDTO;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
@Service
public interface CompanyService {
    GenericDataDTO createCompany(CompanyDTO company);

    GenericDataDTO updateCompany(Integer id, CompanyDTO company);

    GenericDataDTO getCompanyById(String uuid);

    GenericDataDTO getAllCompanies();

    GenericDataDTO deleteCompany(Integer id);

    void createCompanyAssets(List<CompanyAssetDTO> companyAssets, MultipartFile[] files);

    String getAssetAsBase64(Integer companyId, String assetType);
}
