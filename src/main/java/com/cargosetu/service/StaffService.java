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
import com.cargosetu.utils.CommonConstants;
import com.cargosetu.utils.UUIDGenerator;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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
//
//        staff.setEmail(staffDTO.getEmail());
//        staff.setFirstName(staffDTO.getFirstName());
//        staff.setLastName(staffDTO.getLastName());
//        staff.setUserName(staffDTO.getUserName());
//        staff.setMobileCode(staffDTO.getMobileCode());
//        staff.setMobileNumber(staffDTO.getMobileNumber());
//        staff.setPassword(staffDTO.getPassword());

        // set user staff default role
        Optional<Role> companyAdminDefaultRole = rolesRepo.findByCode(CommonConstants.RolesConstants.COMPANY_ADMIN);
        if(companyAdminDefaultRole.isPresent()){
            staff.setRoles(Set.of(companyAdminDefaultRole.get()));
        }else{
            Role role = rolesService.createCompanyAdminDefaultRole();
            staff.setRoles(Set.of(role));
        }
        Staff savedStaff = staffRepository.save(staff);
        staffDTO = staffMapper.toDto(savedStaff);
//        staffDTO = convertStaffToDTO(savedStaff);
        return staffDTO;
    }



    public StaffDTO getStaffById(Integer staffId) {
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
        if(staffDTO.getUserName() == null || staffDTO.getUserName().isBlank()){
            throw new CustomValidationException(HttpStatus.EXPECTATION_FAILED.value(),"please enter your preferred Username");
        }
        return true;
    }
}
