package com.cargosetu.service.impl;

import com.cargosetu.dto.CompanyDTO;
import com.cargosetu.model.Company;
import com.cargosetu.repo.CompanyRepository;
import com.cargosetu.service.CompanyService;
import com.cargosetu.utils.GenericDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@Slf4j
public class CompanyServImpl implements CompanyService {
    @Autowired
    private  CompanyRepository companyRepository;

    @Override
    public GenericDataDTO createCompany(CompanyDTO companyDto) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            log.info("Creating company with email {}", companyDto.getCompanyEmail());

            if (companyRepository.existsByCompanyEmail(companyDto.getCompanyEmail())) {
                response.setResponseMessege("Company email already exists");
                response.setResponseCode(409);
                return response;
            }
            Company company = new Company(companyDto);
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

            existing.setCompanyName(company.getCompanyName());
            existing.setCompanyEmail(company.getCompanyEmail());
            existing.setWhatsappNumber(company.getWhatsappNumber());
            existing.setContactNo1(company.getContactNo1());
            existing.setContactNo2(company.getContactNo2());
            existing.setAddress(company.getAddress());
            existing.setCity(company.getCity());
            existing.setState(company.getState());
            existing.setCountry(company.getCountry());

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
    public GenericDataDTO getCompanyById(Integer id) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            log.info("Fetching company {}", id);

            Company company = companyRepository.findById(id).orElse(null);
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

            companyRepository.deleteById(id);

            response.setResponseMessege("Company deleted successfully");
            response.setResponseCode(200);

        } catch (Exception e) {
            log.error("Error deleting company {}", id, e);
            response.setResponseMessege("Failed to delete company");
            response.setResponseCode(500);
        }
        return response;
    }
}
