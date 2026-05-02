package com.cargosetu.service.impl;

import com.cargosetu.dto.CompanyAssetDTO;
import com.cargosetu.dto.CompanyDTO;
import com.cargosetu.model.Company;
import com.cargosetu.repo.CompanyRepository;
import com.cargosetu.service.CompanyService;
import com.cargosetu.utils.GenericDataDTO;
import com.cargosetu.utils.UUIDGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class CompanyServImpl implements CompanyService {
    @Autowired
    private  CompanyRepository companyRepository;

    @Override
    public GenericDataDTO createCompany(CompanyDTO companyDto) {
        GenericDataDTO response = new GenericDataDTO();
        String prefix = "COPTIC";
        String suffix = "6293";
        try {
            log.info("Creating company with email {}", companyDto.getCompanyEmail());

            if (companyRepository.existsByCompanyEmail(companyDto.getCompanyEmail())) {
                response.setResponseMessege("Company email already exists");
                response.setResponseCode(409);
                return response;
            }
            if(companyDto.getCompanyName().length() >= 5){
                prefix = companyDto.getCompanyName().substring(0,4).toUpperCase();
            }
            StringBuilder rev = new StringBuilder(companyDto.getContactNo1().toString()).reverse();
            if(!rev.isEmpty()){
                suffix = rev.substring(0,4);
            }
            Company company = new Company(companyDto);
            company.setCompanyUUID(UUIDGenerator.generate(prefix,suffix));
            Company saved = companyRepository.save(company);

            response.setResponseMessege("Company created successfully");
            response.setResponseCode(200);
            response.setData(saved);

        } catch (Exception e) {
            log.error("Error creating company", e);
            response.setResponseMessege("Failed to create company");
            response.setResponseCode(HttpStatus.OK.value());
        }
        return response;
    }

    @Override
    public GenericDataDTO updateCompany(Integer id, CompanyDTO company) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            log.info("Updating company ID {}", id);

            Company existing = companyRepository.findById(id).orElse(null);
            if (existing == null) {
                response.setResponseMessege("Company not found");
                response.setResponseCode(404);
                return response;
            }

            if (company.getCompanyName() != null) {
                existing.setCompanyName(company.getCompanyName());
            }

            if (company.getCompanyEmail() != null) {
                existing.setCompanyEmail(company.getCompanyEmail());
            }

            if (company.getWhatsappNumber() != null) {
                existing.setWhatsappNumber(company.getWhatsappNumber());
            }

            if (company.getContactNo1() != null) {
                existing.setContactNo1(company.getContactNo1());
            }

            if (company.getContactNo2() != null) {
                existing.setContactNo2(company.getContactNo2());
            }

            if (company.getAddress() != null) {
                existing.setAddress(company.getAddress());
            }

            if (company.getCity() != null) {
                existing.setCity(company.getCity());
            }

            if (company.getState() != null) {
                existing.setState(company.getState());
            }

            if (company.getCountry() != null) {
                existing.setCountry(company.getCountry());
            }

            existing.setUpdatedAt(LocalDateTime.now());

            Company updated = companyRepository.save(existing);

            response.setResponseMessege("Company updated successfully");
            response.setResponseCode(200);
            response.setData(updated);

        } catch (Exception e) {
            log.error("Error updating company {}", id, e);
            response.setResponseMessege("Failed to update company");
            response.setResponseCode(500);
        }
        return response;
    }

    @Override
    public GenericDataDTO getCompanyById(String id) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            log.info("Fetching company {}", id);

            Company company = companyRepository.findByCompanyUUIDAndIsDeleted(id, false).orElse(null);
            if (company == null) {
                response.setResponseMessege("Company not found");
                response.setResponseCode(404);
                return response;
            }

            response.setResponseMessege("Company fetched successfully");
            response.setResponseCode(200);
            response.setData(company);

        } catch (Exception e) {
            log.error("Error fetching company {}", id, e);
            response.setResponseMessege("Failed to fetch company");
            response.setResponseCode(500);
        }
        return response;
    }

    @Override
    public GenericDataDTO getAllCompanies() {
        GenericDataDTO response = new GenericDataDTO();
        try {
            log.info("Fetching all companies");

            List<Company> list = companyRepository.findAll();

            response.setResponseMessege("Companies fetched successfully");
            response.setResponseCode(200);
            response.setDataList(list);

        } catch (Exception e) {
            log.error("Error fetching companies", e);
            response.setResponseMessege("Failed to fetch companies");
            response.setResponseCode(500);
        }
        return response;
    }

    @Override
    public GenericDataDTO deleteCompany(Integer id) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            log.info("Deleting company {}", id);

            if (!companyRepository.existsById(id)) {
                response.setResponseMessege("Company not found");
                response.setResponseCode(404);
                return response;
            }

            Optional<Company> company = companyRepository.findById(id);
            if(company.isPresent()){
                company.get().setIsActive(false);
                company.get().setIsDeleted(true);
                companyRepository.save(company.get());
            }
            response.setResponseMessege("Company deleted successfully");
            response.setResponseCode(200);

        } catch (Exception e) {
            log.error("Error deleting company {}", id, e);
            response.setResponseMessege("Failed to delete company");
            response.setResponseCode(500);
        }
        return response;
    }

    @Override
    public void createCompanyAssets(List<CompanyAssetDTO> companyAssets) {

    }
}
