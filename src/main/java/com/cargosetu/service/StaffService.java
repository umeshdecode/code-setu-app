package com.cargosetu.service;

import com.cargosetu.dto.RolesDTO;
import com.cargosetu.dto.StaffDTO;
import com.cargosetu.handler.CustomValidationException;
import com.cargosetu.mapper.StaffMapper;
import com.cargosetu.model.Role;
import com.cargosetu.model.Staff;
import com.cargosetu.repo.RolesRepo;
import com.cargosetu.repository.StaffRepository;
import com.cargosetu.service.impl.RolesImpl;
import com.cargosetu.utils.ApplicationUtils;
import com.cargosetu.utils.CommonConstants;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.UUIDGenerator;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
public class StaffService {

    private final StaffRepository staffRepository;
    private final RolesRepo rolesRepo;
    private final RolesImpl rolesService;
    private final StaffMapper staffMapper;

    public StaffService(StaffRepository staffRepository, RolesRepo rolesRepo, RolesImpl rolesService, StaffMapper staffMapper) {
        this.staffRepository = staffRepository;
        this.rolesRepo = rolesRepo;
        this.rolesService = rolesService;
        this.staffMapper = staffMapper;
    }

    private String getModuleName(){
        return "Staff[StaffService]";
    }

    @Transactional
    public StaffDTO saveStaff(StaffDTO staffDTO){
        Staff staff = new Staff();

        String generatedUUID = UUIDGenerator.generate();
        staff = staffMapper.toEntity(staffDTO);
        staff.setUuid(generatedUUID);

        // set user staff default role
        Optional<Role> companyAdminDefaultRole = rolesRepo.findByCode(CommonConstants.RolesConstants.COMPANY_ADMIN);
        if(companyAdminDefaultRole.isPresent()){
            staff.setRoles(Set.of(companyAdminDefaultRole.get()));
        }else{
            Role role = rolesService.createCompanyAdminDefaultRole();
            staff.setRoles(Set.of(role));
        }
         String encode = new BCryptPasswordEncoder().encode(staff.getPassword());
        staff.setPassword(encode);
        Staff savedStaff = staffRepository.save(staff);
        staffDTO = staffMapper.toDto(savedStaff);
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
            rolesDTO.setDiscription(role.getDiscription());
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
        if(Objects.isNull(staffDTO )){
            return false;
        }
        return true;
    }
}
