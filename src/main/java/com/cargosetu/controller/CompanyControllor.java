package com.cargosetu.controller;

import com.cargosetu.dto.CompanyDTO;
import com.cargosetu.service.CompanyService;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.GenericDataDTO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).COMPANY_UPDATE_DELETE)")
    @PutMapping
    public GenericDataDTO updateCompany(
            @Valid @RequestBody CompanyDTO company) {

        log.info("API: Update Company {}", company.getCompanyId());
        return companyService.updateCompany(company.getCompanyId(), company);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).COMPANY_READ)")
    @GetMapping("/{uuid}")
    public GenericDataDTO getCompany(@PathVariable String uuid) {
        log.info("API: Get Company {}", uuid);
        return companyService.getCompanyById(uuid);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).COMPANY_READ)")
    @GetMapping
    public GenericDataDTO getAllCompanies() {
        log.info("API: Get All Companies");
        return companyService.getAllCompanies();
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).COMPANY_UPDATE_DELETE)")
    @DeleteMapping("/{id}")
    public GenericDataDTO deleteCompany(@PathVariable Integer id) {
        log.info("API: Delete Company {}", id);
        return companyService.deleteCompany(id);

    }
}
