package com.cargosetu.controller;

import com.cargosetu.config.utils.UrlConstants;
import com.cargosetu.dto.CompanyDTO;
import com.cargosetu.model.Company;
import com.cargosetu.service.CompanyService;
import com.cargosetu.urls.BaseURL;
import com.cargosetu.utils.GenericDataDTO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(BaseURL.BASE_URL+BaseURL.COMPANY)
@Slf4j
public class CompanyControllor {
    @Autowired
    private CompanyService companyService;

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).COMPANY_CREATE)")
    @PostMapping
    public GenericDataDTO createCompany(@Valid @RequestBody CompanyDTO company) {
        log.info("API: Create Company");
        return companyService.createCompany(company);
    }
    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).COMPANY_UPDATE)")
    @PutMapping("/{id}")
    public GenericDataDTO updateCompany(
            @PathVariable Integer id,
            @Valid @RequestBody CompanyDTO company) {

        log.info("API: Update Company {}", id);
        return companyService.updateCompany(id, company);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).COMPANY_READ)")
    @GetMapping("/{id}")
    public GenericDataDTO getCompany(@PathVariable Integer id) {
        log.info("API: Get Company {}", id);
        return companyService.getCompanyById(id);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).COMPANY_READ)")
    @GetMapping
    public GenericDataDTO getAllCompanies() {
        log.info("API: Get All Companies");
        return companyService.getAllCompanies();
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).COMPANY_DELETE)")
    @DeleteMapping("/{id}")
    public GenericDataDTO deleteCompany(@PathVariable Integer id) {
        log.info("API: Delete Company {}", id);
        return companyService.deleteCompany(id);

    }
}
