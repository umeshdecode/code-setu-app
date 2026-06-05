package com.cargosetu.controller;

import com.cargosetu.dto.ValidationResponse;
import com.cargosetu.model.Branch;
import com.cargosetu.service.BranchService;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(BaseURL.BASE_URL + BaseURL.BRANCHES)
@Slf4j
public class BranchController {
    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).COMPANY_CREATE)")
    @PostMapping
    public ResponseEntity<?> createBranch(@RequestBody Branch branch) {
        log.info("API: Create branch");
        GenericDataDTO response = new GenericDataDTO();
        try {
            ValidationResponse validationResponse = branchService.validateRequest(branch);
            if (validationResponse.isValid()) {
                response = branchService.createBranch(branch);
            } else {
                response.setResponseCode(CommonStatusCode.VALIDATION_ERROR);
                response.setResponseMessege(validationResponse.getMessage());
            }
        } catch (Exception e) {
            log.error("Error creating branch: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).COMPANY_CREATE)")
    @PutMapping("/{branchId}")
    public ResponseEntity<?> updateBranch(@PathVariable Long branchId, @RequestBody Branch branch) {
        log.info("API: Update branch with ID: {}", branchId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            // Assuming branchId from path takes precedence or is used for validation
            branch.setBranchId(branchId);
            ValidationResponse validationResponse = branchService.validateUpdateRequest(branch);
            if (validationResponse.isValid()) {
                response = branchService.updateBranch(branch);
            } else {
                response.setResponseCode(CommonStatusCode.VALIDATION_ERROR);
                response.setResponseMessege(validationResponse.getMessage());
            }
        } catch (Exception e) {
            log.error("Error updating branch with ID {}: {}", branchId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).COMPANY_CREATE)")
    @GetMapping("/{branchId}")
    public ResponseEntity<?> getBranch(@PathVariable Long branchId) {
        log.info("API: Get branch with ID: {}", branchId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = branchService.getBranch(branchId);
        } catch (Exception e) {
            log.error("Error getting branch with ID {}: {}", branchId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).COMPANY_CREATE)")
    @DeleteMapping("/{branchId}")
    public ResponseEntity<?> deleteBranch(@PathVariable Long branchId) {
        log.info("API: Delete branch with ID: {}", branchId);
        GenericDataDTO response = new GenericDataDTO();
        try {
            response = branchService.deleteBranch(branchId);
        } catch (Exception e) {
            log.error("Error deleting branch with ID {}: {}", branchId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setErrors(new Object[]{e.getMessage()});
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok(response);
    }
}
