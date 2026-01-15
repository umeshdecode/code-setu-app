package com.cargosetu.service;

import com.cargosetu.dto.RolesDTO;
import com.cargosetu.dto.StaffDTO;
import com.cargosetu.model.Role;
import com.cargosetu.model.Staff;
import com.cargosetu.repository.StaffRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class StaffService {

    private final StaffRepository staffRepository;

    public StaffService(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    private String getModuleName(){
        return "Staff[StaffService]";
    }

    @Transactional
    public Boolean saveStaff(){
        return false;
    }

    public StaffDTO getStaffById(Integer staffId) {
        Optional<Staff> staff = staffRepository.findById(staffId);
        if(staff.isPresent()){
            return convertStaffToDTO(staff.get());
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
}
