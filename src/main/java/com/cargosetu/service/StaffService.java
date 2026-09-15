package com.cargosetu.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.dto.RolesDTO;
import com.cargosetu.dto.StaffDTO;
import com.cargosetu.dto.UserPreferencesDTO;
import com.cargosetu.handler.CustomValidationException;
import com.cargosetu.mapper.PermissionMapper;
import com.cargosetu.mapper.StaffMapper;
import com.cargosetu.model.Department;
import com.cargosetu.model.Role;
import com.cargosetu.model.Staff;
import com.cargosetu.repo.DepartmentRepo;
import com.cargosetu.repo.RolesRepo;
import com.cargosetu.repo.StaffRepository;
import com.cargosetu.service.impl.RolesImpl;
import com.cargosetu.utils.ApplicationUtils;
import com.cargosetu.utils.CommonConstants;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.UUIDGenerator;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.context.ApplicationEventPublisher;
import com.cargosetu.dto.S3UploadResult;
import com.cargosetu.notification.event.GenericNotificationEvent;
import com.cargosetu.notification.model.NotificationChannel;
import java.util.Map;
import java.util.List;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StaffService {

    /** Bucket config key (tbl_s3_bucket_config) used for staff profile photos. */
    public static final String PROFILE_IMAGE_CONFIG_KEY = "PROFILE_IMAGES";

    /** Image types accepted for a profile photo. */
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    /** Hard cap on the uploaded photo size (5 MB). */
    private static final long MAX_PROFILE_IMAGE_BYTES = 5L * 1024 * 1024;

    /** Matches the UI default so a user who never opened the theme panel still gets a valid mode. */
    private static final String DEFAULT_THEME_MODE = "system";

    private static final com.fasterxml.jackson.databind.ObjectMapper preferencesObjectMapper =
            new com.fasterxml.jackson.databind.ObjectMapper();

    private final StaffRepository staffRepository;
    private final RolesRepo rolesRepo;
    private final DepartmentRepo departmentRepo;
    private final RolesImpl rolesService;
    private final StaffMapper staffMapper;
    private final PermissionMapper permissionMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final S3StorageService s3StorageService;

    public StaffService(StaffRepository staffRepository, RolesRepo rolesRepo, DepartmentRepo departmentRepo, RolesImpl rolesService, StaffMapper staffMapper, PermissionMapper permissionMapper, ApplicationEventPublisher eventPublisher, S3StorageService s3StorageService) {
        this.staffRepository = staffRepository;
        this.rolesRepo = rolesRepo;
        this.departmentRepo = departmentRepo;
        this.rolesService = rolesService;
        this.staffMapper = staffMapper;
        this.permissionMapper = permissionMapper;
        this.eventPublisher = eventPublisher;
        this.s3StorageService = s3StorageService;
    }

    private String getModuleName(){
        return "Staff[StaffService]";
    }

    private String getCurrentUserName(LoggedInUser user){
        if(user == null){
            return "-";
        }
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";
        return (firstName + " " + lastName).isBlank() ? "-" : firstName + " " + lastName;
    }

    @Transactional
    public StaffDTO saveStaff(StaffDTO staffDTO,boolean isAdmin){
        validateCreateStaffRequest(staffDTO);

        String generatedUUID = UUIDGenerator.generate();
        Staff staff = staffMapper.toEntity(staffDTO);
        staff.setUuid(generatedUUID);
        String userNamePrefix = (staffDTO.getEmail() != null && staffDTO.getEmail().contains("@"))
                ? staffDTO.getEmail().substring(0, staffDTO.getEmail().indexOf("@"))
                : staffDTO.getEmail();
        staff.setUserName(userNamePrefix + staff.getUuid().substring(4));
        // set user staff default role
        if(isAdmin){
            Optional<Role> companyAdminDefaultRole = rolesRepo.findByCode(CommonConstants.RolesConstants.COMPANY_ADMIN);
            if(companyAdminDefaultRole.isPresent()){
                staff.setRoles(Set.of(companyAdminDefaultRole.get()));
            }else{
                Role role = rolesService.createCompanyAdminDefaultRole();
                staff.setRoles(Set.of(role));
            }
            if(staffDTO.getPassword() == null || staffDTO.getPassword().isBlank()){
                throw new CustomValidationException(CommonStatusCode.VALIDATION_ERROR,"Password is required");
            }
            // Self-signup always creates the company admin, so staffType must reflect that
            // designation rather than whatever placeholder the signup form happened to send.
            staff.setStaffType(CommonConstants.RolesConstants.COMPANY_ADMIN);
        }else {
            List<Role> roleAndPermissions = staffDTO.getRoleIds() != null
                    ? rolesRepo.findAllById(staffDTO.getRoleIds())
                    : Collections.emptyList();
            
            LoggedInUser currentUser = ApplicationUtils.getCurrentUser();
            Integer staffCompanyId = staffDTO.getCompanyId() != null ? staffDTO.getCompanyId() : (currentUser != null ? currentUser.getCompanyId() : null);
            for (Role r : roleAndPermissions) {
                if (r.getCompanyId() != null && !r.getCompanyId().equals(staffCompanyId)) {
                    throw new CustomValidationException(CommonStatusCode.VALIDATION_ERROR, "Cannot assign roles belonging to another company");
                }
            }

            staff.setRoles(new HashSet<>(roleAndPermissions));
            if(staffDTO.getPassword() == null || staffDTO.getPassword().isBlank()){
                staffDTO.setPassword(ApplicationUtils.generateRandomPassword(10));
            }
        }

        String encode = new BCryptPasswordEncoder().encode(staffDTO.getPassword());
        staff.setPassword(encode);

        staff.setDepartment(null);
        if(staffDTO.getDepartment() != null && staffDTO.getDepartment().getDepartmentId() != null){
            departmentRepo.findById(staffDTO.getDepartment().getDepartmentId()).ifPresent(staff::setDepartment);
        }

        LoggedInUser currentUser = ApplicationUtils.getCurrentUser();
        if(currentUser != null){
            staff.setCreatedBy(currentUser.getStaffId());
            staff.setCreatedByName(getCurrentUserName(currentUser));
            staff.setCompanyId(staffDTO.getCompanyId() != null
                    ? staffDTO.getCompanyId()
                    : currentUser.getCompanyId());
        }
        Staff savedStaff = staffRepository.save(staff);
        staffDTO = withProfileImage(staffMapper.toDto(savedStaff));

        // Publish generic email and SMS events
        Map<String, Object> variables = Map.of(
            "staffId", savedStaff.getStaffId(),
            "email", savedStaff.getEmail() != null ? savedStaff.getEmail() : "",
            "name", (savedStaff.getFirstName() != null ? savedStaff.getFirstName() : "") + " " + (savedStaff.getLastName() != null ? savedStaff.getLastName() : ""),
            "username", savedStaff.getUserName() != null ? savedStaff.getUserName() : ""
        );
        if (savedStaff.getEmail() != null && !savedStaff.getEmail().isBlank()) {
            eventPublisher.publishEvent(new GenericNotificationEvent(
                "STAFF_CREATED",
                List.of(savedStaff.getEmail()),
                NotificationChannel.EMAIL,
                variables
            ));
        }
        if (savedStaff.getMobileNumber() != null && !savedStaff.getMobileNumber().isBlank()) {
            eventPublisher.publishEvent(new GenericNotificationEvent(
                "STAFF_CREATED",
                List.of(savedStaff.getMobileNumber()),
                NotificationChannel.SMS,
                variables
            ));
        }

        return staffDTO;
    }



    public StaffDTO getStaffById(Long staffId) {
        Optional<Staff> staff = staffRepository.findById(staffId);
        if(staff.isPresent()){
            return withProfileImage(staffMapper.toDto(staff.get()));
        }
        return null;
    }

    public StaffDTO getMyProfile(Long staffId) {
        Optional<Staff> staff = staffRepository.findByStaffIdAndIsDeleted(staffId, false);
        if(staff.isPresent()){
            return withProfileImage(staffMapper.toDto(staff.get()));
        }
        return null;
    }

    // ── Profile image ─────────────────────────────────────────────────────────

    /**
     * Moves the persisted S3 object key into {@code profileImageKey} and replaces
     * {@code profileImageUrl} with a viewable (public or presigned) URL so clients can
     * render the photo directly. Safe to call with a null/blank key.
     */
    private StaffDTO withProfileImage(StaffDTO dto) {
        if (dto == null) {
            return null;
        }
        String storedKey = dto.getProfileImageUrl();
        dto.setProfileImageKey(storedKey);
        if (storedKey == null || storedKey.isBlank()) {
            dto.setProfileImageUrl(null);
            return dto;
        }
        // Already an absolute URL (legacy rows) — hand it back untouched.
        if (storedKey.startsWith("http://") || storedKey.startsWith("https://")) {
            return dto;
        }
        try {
            dto.setProfileImageUrl(s3StorageService.getViewableUrl(storedKey, PROFILE_IMAGE_CONFIG_KEY));
        } catch (Exception e) {
            log.warn("Could not build viewable URL for profile image key {}: {}", storedKey, e.getMessage());
            dto.setProfileImageUrl(null);
        }
        return dto;
    }

    /**
     * Uploads (or replaces) a staff member's profile photo in S3 and persists the new
     * object key. The previous object, if any, is deleted on a best-effort basis.
     *
     * @return the updated staff profile with a viewable image URL, or {@code null} if no such staff
     */
    @Transactional
    public StaffDTO saveProfileImage(Long staffId, MultipartFile file) {
        if (staffId == null || staffId <= 0) {
            throw new CustomValidationException(CommonStatusCode.VALIDATION_ERROR, "Invalid staff id");
        }
        validateProfileImage(file);

        Staff staff = staffRepository.findByStaffIdAndIsDeleted(staffId, false).orElse(null);
        if (staff == null) {
            return null;
        }

        String oldKey = staff.getProfileImageUrl();
        try {
            S3UploadResult uploaded = s3StorageService.uploadFile(
                    file, PROFILE_IMAGE_CONFIG_KEY, "staff/" + staffId + "/");
            staff.setProfileImageUrl(uploaded.getS3Key());
        } catch (Exception e) {
            log.error("Profile image upload failed for staffId={}", staffId, e);
            throw new CustomValidationException(CommonStatusCode.INTERNAL_SERVER_ERROR,
                    "Profile image upload failed: " + e.getMessage());
        }

        stampAudit(staff);
        Staff saved = staffRepository.save(staff);
        deleteFromS3Quietly(oldKey);
        log.info("Profile image updated for staffId={} key={}", staffId, saved.getProfileImageUrl());

        return withProfileImage(staffMapper.toDto(saved));
    }

    /**
     * Removes a staff member's profile photo from S3 and clears the stored key.
     *
     * @return the updated staff profile, or {@code null} if no such staff
     */
    @Transactional
    public StaffDTO removeProfileImage(Long staffId) {
        if (staffId == null || staffId <= 0) {
            throw new CustomValidationException(CommonStatusCode.VALIDATION_ERROR, "Invalid staff id");
        }
        Staff staff = staffRepository.findByStaffIdAndIsDeleted(staffId, false).orElse(null);
        if (staff == null) {
            return null;
        }
        String oldKey = staff.getProfileImageUrl();
        staff.setProfileImageUrl(null);
        stampAudit(staff);
        Staff saved = staffRepository.save(staff);
        deleteFromS3Quietly(oldKey);
        log.info("Profile image removed for staffId={}", staffId);
        return withProfileImage(staffMapper.toDto(saved));
    }

    private void validateProfileImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomValidationException(CommonStatusCode.VALIDATION_ERROR, "Profile image file is required");
        }
        if (file.getSize() > MAX_PROFILE_IMAGE_BYTES) {
            throw new CustomValidationException(CommonStatusCode.VALIDATION_ERROR,
                    "Profile image must be 5 MB or smaller");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new CustomValidationException(CommonStatusCode.VALIDATION_ERROR, "Profile image file name is invalid");
        }
        int dotIndex = fileName.lastIndexOf('.');
        String extension = dotIndex > 0 ? fileName.substring(dotIndex + 1).toLowerCase() : "";
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new CustomValidationException(CommonStatusCode.VALIDATION_ERROR,
                    "Only jpg, jpeg, png and webp images are allowed");
        }
    }

    /** Best-effort S3 cleanup — a failed delete must not roll back the profile update. */
    private void deleteFromS3Quietly(String s3Key) {
        if (s3Key == null || s3Key.isBlank()
                || s3Key.startsWith("http://") || s3Key.startsWith("https://")) {
            return;
        }
        try {
            s3StorageService.deleteFile(s3Key, PROFILE_IMAGE_CONFIG_KEY);
            log.info("Deleted old profile image from S3: {}", s3Key);
        } catch (Exception e) {
            log.warn("Failed to delete old profile image {}: {}", s3Key, e.getMessage());
        }
    }

    private void stampAudit(Staff staff) {
        LoggedInUser currentUser = ApplicationUtils.getCurrentUser();
        staff.setUpdatedAt(ApplicationUtils.getCurrentDateTime());
        staff.setUpdatedByName(getCurrentUserName(currentUser));
        if (currentUser != null) {
            staff.setUpdatedById(currentUser.getStaffId());
        }
    }

    /**
     * Self-service profile update. Only fields a staff member is allowed to change about
     * themselves are applied — roles, permissions, company, salary, staff type, active flag
     * and password are deliberately untouched here.
     *
     * @return the updated profile, or {@code null} if no such staff
     */
    @Transactional
    public StaffDTO updateMyProfile(Long staffId, StaffDTO staffDTO) {
        if (staffId == null || staffId <= 0) {
            throw new CustomValidationException(CommonStatusCode.VALIDATION_ERROR, "Invalid staff id");
        }
        if (staffDTO == null) {
            throw new CustomValidationException(CommonStatusCode.VALIDATION_ERROR, "Invalid request");
        }
        Staff staff = staffRepository.findByStaffIdAndIsDeleted(staffId, false).orElse(null);
        if (staff == null) {
            return null;
        }

        if (staffDTO.getFirstName() != null && staffDTO.getFirstName().isBlank()) {
            throw new CustomValidationException(CommonStatusCode.VALIDATION_ERROR, "First name cannot be blank");
        }
        if (staffDTO.getLastName() != null && staffDTO.getLastName().isBlank()) {
            throw new CustomValidationException(CommonStatusCode.VALIDATION_ERROR, "Last name cannot be blank");
        }
        if (staffDTO.getEmail() != null && !staffDTO.getEmail().isBlank()
                && !staffDTO.getEmail().equalsIgnoreCase(staff.getEmail())) {
            Staff duplicate = staffRepository.findByEmail(staffDTO.getEmail()).orElse(null);
            if (duplicate != null && !duplicate.getStaffId().equals(staffId)) {
                throw new CustomValidationException(CommonStatusCode.VALIDATION_ERROR, "Email already in use");
            }
            staff.setEmail(staffDTO.getEmail());
        }

        applyIfPresent(staffDTO.getFirstName(), staff::setFirstName);
        applyIfPresent(staffDTO.getLastName(), staff::setLastName);
        applyIfPresent(staffDTO.getMobileCode(), staff::setMobileCode);
        applyIfPresent(staffDTO.getMobileNumber(), staff::setMobileNumber);
        applyIfPresent(staffDTO.getAlternateMobile(), staff::setAlternateMobile);
        applyIfPresent(staffDTO.getAddress(), staff::setAddress);
        applyIfPresent(staffDTO.getCity(), staff::setCity);
        applyIfPresent(staffDTO.getState(), staff::setState);
        applyIfPresent(staffDTO.getPincode(), staff::setPincode);
        applyIfPresent(staffDTO.getLicenseNumber(), staff::setLicenseNumber);
        if (staffDTO.getLicenseExpiryDate() != null) {
            staff.setLicenseExpiryDate(staffDTO.getLicenseExpiryDate());
        }
        if (staffDTO.getProfileImageKey() != null) {
            staff.setProfileImageUrl(staffDTO.getProfileImageKey().isBlank() ? null : staffDTO.getProfileImageKey().trim());
        } else if (staffDTO.getProfileImageUrl() != null) {
            staff.setProfileImageUrl(staffDTO.getProfileImageUrl().isBlank() ? null : staffDTO.getProfileImageUrl().trim());
        }

        stampAudit(staff);
        Staff saved = staffRepository.save(staff);
        log.info("Self profile updated for staffId={}", staffId);
        return withProfileImage(staffMapper.toDto(saved));
    }

    /**
     * Reads the logged-in user's stored UI preferences.
     *
     * @return the stored preferences, defaults when nothing has been saved yet,
     *         or {@code null} if no such staff
     */
    public UserPreferencesDTO getMyPreferences(Long staffId) {
        if (staffId == null || staffId <= 0) {
            throw new CustomValidationException(CommonStatusCode.VALIDATION_ERROR, "Invalid staff id");
        }
        Staff staff = staffRepository.findByStaffIdAndIsDeleted(staffId, false).orElse(null);
        if (staff == null) {
            return null;
        }
        return readPreferences(staff);
    }

    /**
     * Partial update of the logged-in user's UI preferences. Fields the client omits keep
     * their stored value, so a client that only knows about {@code mode} cannot wipe the font.
     *
     * @return the updated preferences, or {@code null} if no such staff
     */
    @Transactional
    public UserPreferencesDTO updateMyPreferences(Long staffId, UserPreferencesDTO preferencesDTO) {
        if (staffId == null || staffId <= 0) {
            throw new CustomValidationException(CommonStatusCode.VALIDATION_ERROR, "Invalid staff id");
        }
        if (preferencesDTO == null) {
            throw new CustomValidationException(CommonStatusCode.VALIDATION_ERROR, "Invalid request");
        }
        Staff staff = staffRepository.findByStaffIdAndIsDeleted(staffId, false).orElse(null);
        if (staff == null) {
            return null;
        }

        UserPreferencesDTO merged = readPreferences(staff);
        applyIfPresent(preferencesDTO.getMode(), merged::setMode);
        applyIfPresent(preferencesDTO.getFontFamily(), merged::setFontFamily);
        applyIfPresent(preferencesDTO.getPrimaryColor(), merged::setPrimaryColor);

        try {
            staff.setThemeSettings(preferencesObjectMapper.writeValueAsString(merged));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new CustomValidationException(CommonStatusCode.VALIDATION_ERROR, "Unable to save preferences");
        }

        stampAudit(staff);
        staffRepository.save(staff);
        log.info("Preferences updated for staffId={}", staffId);
        return merged;
    }

    /**
     * Preferences are stored as free-form JSON, so a blob written by an older or newer client
     * must never break login — an unreadable value degrades to defaults rather than throwing.
     */
    private UserPreferencesDTO readPreferences(Staff staff) {
        String stored = staff.getThemeSettings();
        if (stored == null || stored.isBlank()) {
            return defaultPreferences();
        }
        try {
            UserPreferencesDTO preferences = preferencesObjectMapper.readValue(stored, UserPreferencesDTO.class);
            if (preferences.getMode() == null || preferences.getMode().isBlank()) {
                preferences.setMode(DEFAULT_THEME_MODE);
            }
            return preferences;
        } catch (Exception e) {
            log.warn("Unreadable theme_settings for staffId={}, falling back to defaults: {}",
                    staff.getStaffId(), e.getMessage());
            return defaultPreferences();
        }
    }

    private UserPreferencesDTO defaultPreferences() {
        UserPreferencesDTO preferences = new UserPreferencesDTO();
        preferences.setMode(DEFAULT_THEME_MODE);
        return preferences;
    }

    /** Applies a value only when the client actually sent one (non-null, non-blank). */
    private void applyIfPresent(String value, java.util.function.Consumer<String> setter) {
        if (value != null && !value.isBlank()) {
            setter.accept(value.trim());
        }
    }

    private StaffDTO convertStaffToDTO(Staff staff) {
        StaffDTO staffDTO = new StaffDTO();
        staffDTO.setIsActive(staff.getIsActive());
        staffDTO.setStaffId(staffDTO.getStaffId());
        staffDTO.setMobileCode(staff.getMobileCode());
        staffDTO.setEmail(staff.getEmail());
        staffDTO.setUuid(staff.getUuid());
        staffDTO.setFirstName(staff.getFirstName());
        staffDTO.setLastName(staff.getLastName());
        staffDTO.setUserName(staff.getUserName());
        Set<Role> roles = staff.getRoles();
        Set<RolesDTO> rolesDTOs = new HashSet<>();
        for(Role role : roles){
            RolesDTO rolesDTO = new RolesDTO();
            rolesDTO.setRoleName(role.getRoleName());
            rolesDTO.setCode(role.getCode());
            rolesDTO.setPermissions(role.getPermissions().stream().map(permissionMapper::toDto).collect(Collectors.toSet()));
            rolesDTO.setScope(role.getScope());
            rolesDTOs.add(rolesDTO);
        }
        staffDTO.setRoles(rolesDTOs);
        return staffDTO;
    }

    public boolean validateCreateStaffRequest(StaffDTO staffDTO) {

        if(staffDTO.getFirstName() == null || staffDTO.getFirstName().isBlank()){
            throw new CustomValidationException(HttpStatus.EXPECTATION_FAILED.value(),"First Name cannot be blank");
        }
        if(staffDTO.getLastName() == null || staffDTO.getLastName().isBlank()){
            throw new CustomValidationException(HttpStatus.EXPECTATION_FAILED.value(),"First Name cannot be blank");
        }
        if(staffDTO.getMobileCode() == null || staffDTO.getMobileCode().isBlank()){
            throw new CustomValidationException(HttpStatus.EXPECTATION_FAILED.value(),"please choose correct mobile country code");
        }
        if(staffDTO.getEmail() == null || staffDTO.getEmail().isBlank()){
            throw new CustomValidationException(HttpStatus.EXPECTATION_FAILED.value(),"email is required!!");
        }

        if(staffDTO.getMobileNumber() == null || staffDTO.getMobileNumber().isBlank()){
            throw new CustomValidationException(HttpStatus.EXPECTATION_FAILED.value(),"please enter valid mobile number");
        }
//        if(staffDTO.getPassword() == null || staffDTO.getPassword().isBlank()){
//            throw new CustomValidationException(HttpStatus.EXPECTATION_FAILED.value(),"Password is required");
//        }
//        if(staffDTO.getUserName() == null || staffDTO.getUserName().isBlank()){
//            throw new CustomValidationException(HttpStatus.EXPECTATION_FAILED.value(),"please enter your preferred Username");
//        }
        return true;
    }

    @Transactional
    public Boolean deleteStaff(Long staffId) {
        if(staffId == null || staffId <= 0){
            throw new CustomValidationException(CommonStatusCode.VALIDATION_ERROR,"Invalid request!!");
        }
        Optional<Staff> dbStaff = staffRepository.findById(staffId);
        if(dbStaff.isPresent()){
            Staff staff = dbStaff.get();
            staff.setIsDeleted(true);
            staff.setUpdatedAt(ApplicationUtils.getCurrentDateTime());
            staff.setUpdatedByName(getCurrentUserName(ApplicationUtils.getCurrentUser()));
            staff.setUpdatedById(ApplicationUtils.getCurrentUser().getStaffId());
            staffRepository.save(staff);
            log.info("Staff has been mark as deleted with staffId : {}", staffId);
            return true;
        }
        return false;
    }

    public Boolean isDuplicateUser(StaffDTO staffDTO) {
        Staff user = staffRepository.findByEmailOrUserName(staffDTO.getEmail(), staffDTO.getUserName()).orElse(null);
        return Objects.nonNull(user);
    }

    @Transactional
    public StaffDTO updateStaff(Long staffId, StaffDTO staffDTO) {
        if (staffId == null || staffId <= 0) {
            throw new CustomValidationException(CommonStatusCode.VALIDATION_ERROR, "Invalid staff id");
        }
        Optional<Staff> dbStaffOpt = staffRepository.findById(staffId);
        if (dbStaffOpt.isEmpty()) {
            return null;
        }
        Staff dbStaff = dbStaffOpt.get();

        Staff duplicate = staffRepository
                .findByEmailOrUserName(staffDTO.getEmail(), staffDTO.getUserName())
                .orElse(null);
        if (duplicate != null && !duplicate.getStaffId().equals(staffId)) {
            throw new CustomValidationException(CommonStatusCode.VALIDATION_ERROR, "Username/email already exists");
        }

        String existingPassword = dbStaff.getPassword();
        String existingUserName = dbStaff.getUserName();

        dbStaff.setFirstName(staffDTO.getFirstName());
        dbStaff.setLastName(staffDTO.getLastName());
        dbStaff.setMobileCode(staffDTO.getMobileCode());
        dbStaff.setMobileNumber(staffDTO.getMobileNumber());
        dbStaff.setEmail(staffDTO.getEmail());
        dbStaff.setStaffType(staffDTO.getStaffType());
        // The profile photo is owned by the dedicated upload/remove endpoints. A read hands
        // clients a presigned URL in profileImageUrl, so only an explicit profileImageKey
        // may change it here — otherwise the stored S3 key is preserved.
        if (staffDTO.getProfileImageKey() != null) {
            dbStaff.setProfileImageUrl(staffDTO.getProfileImageKey().isBlank()
                    ? null
                    : staffDTO.getProfileImageKey());
        } else if (staffDTO.getProfileImageUrl() != null) {
            dbStaff.setProfileImageUrl(staffDTO.getProfileImageUrl().isBlank()
                    ? null
                    : staffDTO.getProfileImageUrl().trim());
        }
        dbStaff.setAlternateMobile(staffDTO.getAlternateMobile());
        dbStaff.setAddress(staffDTO.getAddress());
        dbStaff.setCity(staffDTO.getCity());
        dbStaff.setState(staffDTO.getState());
        dbStaff.setPincode(staffDTO.getPincode());
        dbStaff.setLicenseNumber(staffDTO.getLicenseNumber());
        dbStaff.setLicenseExpiryDate(staffDTO.getLicenseExpiryDate());
        dbStaff.setIsActive(staffDTO.getIsActive());
        dbStaff.setCompanyId(staffDTO.getCompanyId());
        dbStaff.setSalary(staffDTO.getSalary());

        if (staffDTO.getDepartment() != null && staffDTO.getDepartment().getDepartmentId() != null) {
            Optional<Department> department = departmentRepo.findById(staffDTO.getDepartment().getDepartmentId());
            department.ifPresent(dbStaff::setDepartment);
        }

        if (staffDTO.getPassword() == null || staffDTO.getPassword().isBlank()) {
            dbStaff.setPassword(existingPassword);
        } else {
            dbStaff.setPassword(new BCryptPasswordEncoder().encode(staffDTO.getPassword()));
        }
        if (staffDTO.getUserName() == null || staffDTO.getUserName().isBlank()) {
            dbStaff.setUserName(existingUserName);
        } else {
            dbStaff.setUserName(staffDTO.getUserName());
        }
        if (staffDTO.getRoleIds() != null && !staffDTO.getRoleIds().isEmpty()) {
            List<Role> roleAndPermissions = rolesRepo.findAllById(staffDTO.getRoleIds());

            LoggedInUser currentUser = ApplicationUtils.getCurrentUser();
            Integer staffCompanyId = staffDTO.getCompanyId() != null ? staffDTO.getCompanyId() : (currentUser != null ? currentUser.getCompanyId() : null);
            for (Role r : roleAndPermissions) {
                if (r.getCompanyId() != null && !r.getCompanyId().equals(staffCompanyId)) {
                    throw new CustomValidationException(CommonStatusCode.VALIDATION_ERROR, "Cannot assign roles belonging to another company");
                }
            }

            dbStaff.setRoles(new HashSet<>(roleAndPermissions));
        }

        dbStaff.setUpdatedAt(ApplicationUtils.getCurrentDateTime());
        dbStaff.setUpdatedByName(getCurrentUserName(ApplicationUtils.getCurrentUser()));
        dbStaff.setUpdatedById(ApplicationUtils.getCurrentUser().getStaffId());
        Staff savedStaff = staffRepository.save(dbStaff);

        StaffDTO savedDTO = withProfileImage(staffMapper.toDto(savedStaff));
        savedDTO.setRoleIds(staffDTO.getRoleIds());
        return savedDTO;
    }

    @Transactional
    public Page<StaffDTO> getAllStaff(LoggedInUser user,Pageable pageable) throws Exception {

        if(user.getStaffId() == 1){
            return staffRepository.findAll(pageable).map(staff -> withProfileImage(staffMapper.toDto(staff)));
        }
        if(Objects.isNull(user.getCompanyId())){
            throw new Exception("Company not found,create Company first!");
        }
        return staffRepository.findAllByCompanyIdOrderByStaffIdDesc(user.getCompanyId(), pageable)
                .map(staff -> withProfileImage(staffMapper.toDto(staff)));
    }


}
