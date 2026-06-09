package com.cargosetu.controller;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.dto.CompanyAssetDTO;
import com.cargosetu.dto.CompanyDTO;
import com.cargosetu.dto.DocumentUploadDTO;
import com.cargosetu.service.CompanyService;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping(BaseURL.BASE_URL+BaseURL.COMPANY)
@Slf4j
public class CompanyController {
    @Autowired
    private CompanyService companyService;
    @Autowired
    private ObjectMapper objectMapper;

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).COMPANY_CREATE)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GenericDataDTO createCompany(@AuthenticationPrincipal LoggedInUser user,@ModelAttribute CompanyDTO company) {
        log.info("API: Create Company");
        return companyService.createCompany(company,user.getStaffId());
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

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).COMPANY_READ)")
    @GetMapping("/currentCompany")
    public GenericDataDTO getCurrentCompany(@AuthenticationPrincipal LoggedInUser user) {
        GenericDataDTO response = new GenericDataDTO();
        if(Objects.nonNull(user) && user.getCompanyId() != null){
            return companyService.getCompany(user.getCompanyId());
        }
        if(Objects.nonNull(user) && user.getStaffId() !=null){{
            return companyService.getCompanyByStaffId(user.getStaffId());
        }

        }
        response.setResponseCode(404);
        response.setResponseMessege("Company not found");
        return response;
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).COMPANY_CREATE)")
    @PostMapping(value = "/assets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createCompanyAssets(@RequestPart("file") MultipartFile[] files, @RequestPart("data") String payload) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            List<CompanyAssetDTO> companyAssets =
                    objectMapper.readValue(
                            payload,
                            new TypeReference<List<CompanyAssetDTO>>() {}
                    );
            if (Objects.isNull(companyAssets) || companyAssets.isEmpty()) {
                response.setResponseMessege(CommonStatusCode.MSG_BAD_REQUEST);
                response.setResponseCode(CommonStatusCode.FAIL);
                return ResponseEntity.badRequest().body(response);
            }
//            companyService.createCompanyAssets(companyAssets, files);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Company assets created successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.setResponseCode(CommonStatusCode.FAIL);
            response.setResponseMessege(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
