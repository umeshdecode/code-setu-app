package com.cargosetu.controller;

import com.cargosetu.config.utils.UrlConstants;
import com.cargosetu.dto.StaffDTO;
import com.cargosetu.service.StaffService;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.CommonConstants;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import java.util.Objects;

@RestController
@RequestMapping(value = BaseURL.BASE_URL +UrlConstants.StaffUrlConstants.STAFF)
public class StaffController {


    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @PreAuthorize(
            "hasAuthority(T(com.cargosetu.utils.PermissionConstants).STAFF_READ) " +
                    "or hasRole(T(com.cargosetu.utils.CommonConstants.RolesConstants).COMPANY_ADMIN)"
    )
    @GetMapping("/{id}")
    public GenericDataDTO getStaff(@PathVariable("id") Long staffId){
        GenericDataDTO dto = new GenericDataDTO();
        StaffDTO staffById = staffService.getStaffById(staffId);
        if (Objects.nonNull(staffById)) {
            dto.setData(staffById);
            dto.setResponseCode(HttpStatus.OK.value());
            dto.setResponseMessege("Staffed fetched successfully");
        }else {
            dto.setResponseCode(HttpStatus.NOT_FOUND.value());
            dto.setResponseMessege("Staff not found");
        }
        return dto;
    }

    @PreAuthorize(
            "hasAuthority(T(com.cargosetu.utils.PermissionConstants).STAFF_CREATE)"
    )
    @PostMapping
    public GenericDataDTO createStaff(@RequestBody StaffDTO staffDTO){
        GenericDataDTO dto = new GenericDataDTO();
        boolean validated = staffService.validateCreateStaffRequest(staffDTO);
        if(validated){
            Boolean duplicateUser = staffService.isDuplicateUser(staffDTO);
                if (duplicateUser) {
                    dto.setResponseCode(CommonStatusCode.VALIDATION_ERROR);
                    dto.setResponseMessege("Username/email already exists");
                    return dto;
                }
            StaffDTO staff = staffService.saveStaff(staffDTO,false);
            if(Objects.nonNull(staff)){
                dto.setData(staff);
                dto.setResponseCode(CommonStatusCode.CREATED);
                dto.setResponseMessege(CommonConstants.STAFF + CommonStatusCode.MSG_CREATED);
            }else {
                dto.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
                dto.setResponseMessege(CommonStatusCode.MSG_INTERNAL_SERVER_ERROR);
            }
        }
        return dto;
    }

    @DeleteMapping("/{staffId}")
    public GenericDataDTO deleteStaff(@PathVariable("staffId") Long staffId){
        GenericDataDTO dto = new GenericDataDTO();
        Boolean deleteStaff = staffService.deleteStaff(staffId);
        if(deleteStaff){
            dto.setResponseCode(CommonStatusCode.SUCCESS);
            dto.setResponseMessege(CommonStatusCode.MSG_SUCCESS);
            return dto;
        }else {
            dto.setResponseCode(CommonStatusCode.FAIL);
            dto.setResponseMessege("Staff not Deleted");
            return dto;
        }
    }
    @PreAuthorize(
            "hasAuthority(T(com.cargosetu.utils.PermissionConstants).STAFF_READ) " +
                    "or hasRole(T(com.cargosetu.utils.CommonConstants.RolesConstants).COMPANY_ADMIN)"
    )
    @GetMapping
    public GenericDataDTO getAllStaff(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "5") int size) {

        Pageable pageable = PageRequest.of(page, size);

        Page<StaffDTO> staffPage = staffService.getAllStaff(pageable);

        GenericDataDTO dto = new GenericDataDTO();
        dto.setData(staffPage.getContent());
        dto.setResponseCode(HttpStatus.OK.value());
        dto.setResponseMessege("Staff fetched successfully");

        return dto;
    }


}
