package com.cargosetu.service;

import com.cargosetu.dto.CompanyDTO;
import com.cargosetu.model.Company;
import com.cargosetu.utils.GenericDataDTO;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public interface CompanyService {
    GenericDataDTO createCompany(CompanyDTO company);

    GenericDataDTO updateCompany(Integer id, CompanyDTO company);

    GenericDataDTO getCompanyById(String uuid);

    GenericDataDTO getAllCompanies();

    GenericDataDTO deleteCompany(Integer id);
}
