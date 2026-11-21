package com.cargosetu.controller;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.config.utils.UrlConstants;
import com.cargosetu.dto.StaffDTO;
import com.cargosetu.service.StaffService;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.CommonConstants;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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

    @PreAuthorize(
            "hasAuthority(T(com.cargosetu.utils.PermissionConstants).STAFF_UPDATE_DELETE)"
    )
    @PutMapping("/{staffId}")
    public GenericDataDTO updateStaff(@PathVariable("staffId") Long staffId, @RequestBody StaffDTO staffDTO){
        GenericDataDTO dto = new GenericDataDTO();
        StaffDTO updated = staffService.updateStaff(staffId, staffDTO);
        if(Objects.nonNull(updated)){
            dto.setData(updated);
            dto.setResponseCode(CommonStatusCode.SUCCESS);
            dto.setResponseMessege(CommonConstants.STAFF + CommonStatusCode.MSG_UPDATED);
        }else {
            dto.setResponseCode(CommonStatusCode.NOT_FOUND);
            dto.setResponseMessege("Staff not found");
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
    @GetMapping("/myprofile")
    public GenericDataDTO getMyProfile(@AuthenticationPrincipal LoggedInUser user){
        GenericDataDTO dto = new GenericDataDTO();
        if (user == null || user.getStaffId() == null) {
            dto.setResponseCode(HttpStatus.UNAUTHORIZED.value());
            dto.setResponseMessege("Unauthorized");
            return dto;
        }
        StaffDTO staff = staffService.getMyProfile(user.getStaffId());
        if (Objects.nonNull(staff)) {
            dto.setData(staff);
            dto.setResponseCode(HttpStatus.OK.value());
            dto.setResponseMessege("Profile fetched successfully");
        }else {
            dto.setResponseCode(HttpStatus.NOT_FOUND.value());
            dto.setResponseMessege("Staff not found or deleted");
        }
        return dto;
    }

    /**
     * Self-service profile update. A staff member may edit their own contact/address
     * details without needing STAFF_UPDATE_DELETE.
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/myprofile")
    public GenericDataDTO updateMyProfile(@AuthenticationPrincipal LoggedInUser user,
                                         @RequestBody StaffDTO staffDTO){
        GenericDataDTO dto = new GenericDataDTO();
        if (user == null || user.getStaffId() == null) {
            dto.setResponseCode(HttpStatus.UNAUTHORIZED.value());
            dto.setResponseMessege(CommonStatusCode.MSG_UNAUTHORIZED);
            return dto;
        }
        StaffDTO updated = staffService.updateMyProfile(user.getStaffId(), staffDTO);
        if (Objects.nonNull(updated)) {
            dto.setData(updated);
            dto.setResponseCode(CommonStatusCode.SUCCESS);
            dto.setResponseMessege("Profile" + CommonStatusCode.MSG_UPDATED);
        } else {
            dto.setResponseCode(CommonStatusCode.NOT_FOUND);
            dto.setResponseMessege("Staff not found or deleted");
        }
        return dto;
    }

    /** Upload or replace the logged-in user's own profile photo. */
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/myprofile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GenericDataDTO uploadMyProfileImage(@AuthenticationPrincipal LoggedInUser user,
                                              @RequestPart("file") MultipartFile file){
        GenericDataDTO dto = new GenericDataDTO();
        if (user == null || user.getStaffId() == null) {
            dto.setResponseCode(HttpStatus.UNAUTHORIZED.value());
            dto.setResponseMessege(CommonStatusCode.MSG_UNAUTHORIZED);
            return dto;
        }
        return buildProfileImageResponse(staffService.saveProfileImage(user.getStaffId(), file),
                "Profile image" + CommonStatusCode.MSG_UPDATED);
    }

    /** Remove the logged-in user's own profile photo. */
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/myprofile/image")
    public GenericDataDTO deleteMyProfileImage(@AuthenticationPrincipal LoggedInUser user){
        GenericDataDTO dto = new GenericDataDTO();
        if (user == null || user.getStaffId() == null) {
            dto.setResponseCode(HttpStatus.UNAUTHORIZED.value());
            dto.setResponseMessege(CommonStatusCode.MSG_UNAUTHORIZED);
            return dto;
        }
        return buildProfileImageResponse(staffService.removeProfileImage(user.getStaffId()),
                "Profile image" + CommonStatusCode.MSG_DELETED);
    }

    /**
     * Upload or replace another staff member's profile photo. Requires
     * STAFF_UPDATE_DELETE, or the caller acting on their own record.
     */
    @PreAuthorize(
            "hasAuthority(T(com.cargosetu.utils.PermissionConstants).STAFF_UPDATE_DELETE) " +
                    "or (principal?.staffId != null and #staffId == principal.staffId)"
    )
    @PostMapping(value = "/{staffId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GenericDataDTO uploadStaffProfileImage(@PathVariable("staffId") Long staffId,
                                                 @RequestPart("file") MultipartFile file){
        return buildProfileImageResponse(staffService.saveProfileImage(staffId, file),
                "Profile image" + CommonStatusCode.MSG_UPDATED);
    }

    /** Remove another staff member's profile photo. */
    @PreAuthorize(
            "hasAuthority(T(com.cargosetu.utils.PermissionConstants).STAFF_UPDATE_DELETE) " +
                    "or (principal?.staffId != null and #staffId == principal.staffId)"
    )
    @DeleteMapping("/{staffId}/image")
    public GenericDataDTO deleteStaffProfileImage(@PathVariable("staffId") Long staffId){
        return buildProfileImageResponse(staffService.removeProfileImage(staffId),
                "Profile image" + CommonStatusCode.MSG_DELETED);
    }

    private GenericDataDTO buildProfileImageResponse(StaffDTO staff, String successMessage) {
        GenericDataDTO dto = new GenericDataDTO();
        if (Objects.nonNull(staff)) {
            dto.setData(staff);
            dto.setResponseCode(CommonStatusCode.SUCCESS);
            dto.setResponseMessege(successMessage);
        } else {
            dto.setResponseCode(CommonStatusCode.NOT_FOUND);
            dto.setResponseMessege("Staff not found or deleted");
        }
        return dto;
    }

    @PreAuthorize(
            "hasAuthority(T(com.cargosetu.utils.PermissionConstants).STAFF_READ) " +
                    "or hasRole(T(com.cargosetu.utils.CommonConstants.RolesConstants).COMPANY_ADMIN)"
    )
    @GetMapping
    public GenericDataDTO getAllStaff(@AuthenticationPrincipal LoggedInUser user,
                                      @RequestParam(required = false, defaultValue = "0") int page,
                                      @RequestParam(required = false, defaultValue = "5") int size) {

        Pageable pageable = PageRequest.of(page, size);

        GenericDataDTO dto = new GenericDataDTO();
        try {
            Page<StaffDTO> staffPage = staffService.getAllStaff(user,pageable);
            dto.setData(staffPage.getContent());
            dto.setResponseCode(HttpStatus.OK.value());
            dto.setResponseMessege("Staff fetched successfully");
        } catch (Exception e) {
            dto.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            dto.setResponseMessege(e.getMessage());
            e.printStackTrace();
        }

        return dto;
    }


}
