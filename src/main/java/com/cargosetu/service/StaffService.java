package com.cargosetu.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.dto.RolesDTO;
import com.cargosetu.dto.StaffDTO;
import com.cargosetu.handler.CustomValidationException;
import com.cargosetu.mapper.PermissionMapper;
import com.cargosetu.mapper.StaffMapper;
import com.cargosetu.model.Role;
import com.cargosetu.model.Staff;
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
import org.springframework.context.ApplicationEventPublisher;
import com.cargosetu.notification.event.StaffCreatedEvent;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StaffService {

    private final StaffRepository staffRepository;
    private final RolesRepo rolesRepo;
    private final RolesImpl rolesService;
    private final StaffMapper staffMapper;
    private final PermissionMapper permissionMapper;
    private final ApplicationEventPublisher eventPublisher;

    public StaffService(StaffRepository staffRepository, RolesRepo rolesRepo, RolesImpl rolesService, StaffMapper staffMapper, PermissionMapper permissionMapper, ApplicationEventPublisher eventPublisher) {
        this.staffRepository = staffRepository;
        this.rolesRepo = rolesRepo;
        this.rolesService = rolesService;
        this.staffMapper = staffMapper;
        this.permissionMapper = permissionMapper;
        this.eventPublisher = eventPublisher;
    }

    private String getModuleName(){
        return "Staff[StaffService]";
    }

    @Transactional
    public StaffDTO saveStaff(StaffDTO staffDTO,boolean isAdmin){
        Staff staff = new Staff();

        String generatedUUID = UUIDGenerator.generate();
        staff = staffMapper.toEntity(staffDTO);
        staff.setUuid(generatedUUID);
        staff.setUserName(staffDTO.getEmail().substring(0, staffDTO.getEmail().indexOf("@"))+staff.getUuid().substring(4));
        // set user staff default role
        if(isAdmin){
            Optional<Role> companyAdminDefaultRole = rolesRepo.findByCode(CommonConstants.RolesConstants.COMPANY_ADMIN);
            if(companyAdminDefaultRole.isPresent()){
                staff.setRoles(Set.of(companyAdminDefaultRole.get()));
            }else{
                Role role = rolesService.createCompanyAdminDefaultRole();
                staff.setRoles(Set.of(role));
            }
        }else {
            List<Role> roleAndPermissions = rolesRepo.findAllById(staffDTO.getRoleIds());
            staff.setRoles(new HashSet<>(roleAndPermissions));
        }
         String encode = new BCryptPasswordEncoder().encode(staff.getPassword());
        staff.setPassword(encode);
        Staff savedStaff = staffRepository.save(staff);
        staffDTO = staffMapper.toDto(savedStaff);

        // Publish event for asynchronous notification post-commit
        eventPublisher.publishEvent(new StaffCreatedEvent(
            savedStaff.getStaffId(),
            savedStaff.getEmail(),
            savedStaff.getFirstName() + " " + savedStaff.getLastName(),
            savedStaff.getUserName()
        ));

        return staffDTO;
    }



    public StaffDTO getStaffById(Long staffId) {
        Optional<Staff> staff = staffRepository.findById(staffId);
        if(staff.isPresent()){
            return staffMapper.toDto(staff.get());
        }
        return null;
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
            staff.setUpdatedByName((ApplicationUtils.getCurrentUser().getFirstName() != null ? ApplicationUtils.getCurrentUser().getFirstName() : "-")+(ApplicationUtils.getCurrentUser().getFirstName() != null ? ApplicationUtils.getCurrentUser().getLastName() : "-"));
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
    public Page<StaffDTO> getAllStaff(LoggedInUser user,Pageable pageable) throws Exception {

        if(user.getStaffId() == 1){
            return staffRepository.findAll(pageable).map(staffMapper::toDto);
        }
        if(Objects.isNull(user.getCompanyId())){
            throw new Exception("Company not found,create Company first!");
        }
        return staffRepository.findAllByCompanyIdOrderByStaffIdDesc(user.getCompanyId(), pageable).map(staffMapper::toDto);
    }


}
