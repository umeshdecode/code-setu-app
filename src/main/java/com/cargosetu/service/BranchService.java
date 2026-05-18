package com.cargosetu.service;

import com.cargosetu.dto.ValidationResponse;
import com.cargosetu.model.Branch;
import com.cargosetu.repo.BranchRepository;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class BranchService {

    private final BranchRepository branchRepository;

    public BranchService(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    public ValidationResponse validateRequest(Branch branch) {
        ValidationResponse response = new ValidationResponse(true,"");

        if(!StringUtils.hasText(branch.getBranchName())){
            response.setValid(false);
            response.setMessage("Branch name is required");
            return response;
        }
        if(!StringUtils.hasText(branch.getMobile())){
            response.setValid(false);
            response.setMessage("Branch mobile number is required");
            return response;
        }
        return response;
    }

    public ValidationResponse validateUpdateRequest(Branch branch) {
        ValidationResponse response = new ValidationResponse(true,"");

        if (branch.getBranchId() == null) {
            response.setValid(false);
            response.setMessage("Branch ID is required for update");
            return response;
        }

        Optional<Branch> existingBranch = branchRepository.findById(branch.getBranchId());
        if (existingBranch.isEmpty()) {
            response.setValid(false);
            response.setMessage("Branch not found with ID: " + branch.getBranchId());
            return response;
        }

        if(!StringUtils.hasText(branch.getBranchName())){
            response.setValid(false);
            response.setMessage("Branch name is required");
            return response;
        }
        if(!StringUtils.hasText(branch.getMobile())){
            response.setValid(false);
            response.setMessage("Branch mobile number is required");
            return response;
        }
        return response;
    }

    public GenericDataDTO createBranch(Branch branch) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Branch saveBranch = branchRepository.save(branch);
            if(Objects.nonNull(saveBranch) && saveBranch.getBranchId() != null && saveBranch.getBranchId() > 0){
                response.setResponseCode(CommonStatusCode.CREATED);
                response.setResponseMessege("Branch created successfully!");
                response.setData(saveBranch);
            } else {
                response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
                response.setResponseMessege("Failed to create branch!");
            }
        } catch (Exception e) {
            log.error("Error creating branch: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while creating the branch.");
        }
        return response;
    }

    public GenericDataDTO updateBranch(Branch branch) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<Branch> existingBranchOptional = branchRepository.findById(branch.getBranchId());
            if (existingBranchOptional.isPresent()) {
                Branch existingBranch = existingBranchOptional.get();
                existingBranch.setBranchName(branch.getBranchName());
                existingBranch.setBranchCode(branch.getBranchCode());
                existingBranch.setContactPerson(branch.getContactPerson());
                existingBranch.setMobile(branch.getMobile());
                existingBranch.setEmail(branch.getEmail());
                existingBranch.setGstNumber(branch.getGstNumber());
                existingBranch.setAddressLine1(branch.getAddressLine1());
                existingBranch.setAddressLine2(branch.getAddressLine2());
                existingBranch.setCity(branch.getCity());
                existingBranch.setState(branch.getState());
                existingBranch.setCountry(branch.getCountry());
                existingBranch.setPincode(branch.getPincode());
                existingBranch.setHeadOffice(branch.getHeadOffice());
                existingBranch.setActive(branch.getActive());
                existingBranch.setCompanyId(branch.getCompanyId());

                Branch updatedBranch = branchRepository.save(existingBranch);
                response.setResponseCode(CommonStatusCode.SUCCESS);
                response.setResponseMessege("Branch updated successfully!");
                response.setData(updatedBranch);
            } else {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Branch not found with ID: " + branch.getBranchId());
            }
        } catch (Exception e) {
            log.error("Error updating branch with ID {}: {}", branch.getBranchId(), e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while updating the branch.");
        }
        return response;
    }

    public GenericDataDTO getBranch(Long branchId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<Branch> branchOptional = branchRepository.findById(branchId);
            if (branchOptional.isPresent()) {
                response.setResponseCode(CommonStatusCode.SUCCESS);
                response.setResponseMessege("Branch found successfully!");
                response.setData(branchOptional.get());
            } else {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Branch not found with ID: " + branchId);
            }
        } catch (Exception e) {
            log.error("Error getting branch with ID {}: {}", branchId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while retrieving the branch.");
        }
        return response;
    }

    public GenericDataDTO deleteBranch(Long branchId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<Branch> branchOptional = branchRepository.findById(branchId);
            if (branchOptional.isPresent()) {
                Branch branchToDelete = branchOptional.get();
                branchToDelete.setActive(false);
                branchRepository.save(branchToDelete);
                response.setResponseCode(CommonStatusCode.SUCCESS);
                response.setResponseMessege("Branch deleted successfully!");
            } else {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Branch not found with ID: " + branchId);
            }
        } catch (Exception e) {
            log.error("Error deleting branch with ID {}: {}", branchId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while deleting the branch.");
        }
        return response;
    }
}
