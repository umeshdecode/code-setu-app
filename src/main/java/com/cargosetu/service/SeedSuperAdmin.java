package com.cargosetu.service;

import com.cargosetu.model.Role;
import com.cargosetu.model.Staff;
import com.cargosetu.repo.RolesRepo;
import com.cargosetu.repository.StaffRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Optional;

@Configuration
@Slf4j
public class SeedSuperAdmin implements ApplicationRunner {

    public interface SuperAdminConstants {
        String SUPERADMIN = "SUPERADMIN";
    }

    private final StaffRepository staffRepository;
    private final RolesRepo rolesRepo;

    public SeedSuperAdmin(StaffRepository staffRepository, RolesRepo rolesRepo) {
        this.staffRepository = staffRepository;
        this.rolesRepo = rolesRepo;
    }


    @Override
    public void run(ApplicationArguments args) throws Exception {
        Optional<Staff> superAdmin = staffRepository.findByUserName(SuperAdminConstants.SUPERADMIN);
        HashSet<Role> roles = new HashSet<>();
        if(superAdmin.isEmpty()) {
            Role role = null;
            Optional<Role> roleOptional = rolesRepo.findByCode(SuperAdminConstants.SUPERADMIN);
            if(roleOptional.isPresent()) {
                role = roleOptional.get();
            }else{
                role = seedDefaultRole();
            }
            roles.add(role);
            Staff staff = new Staff("abcs123",SuperAdminConstants.SUPERADMIN,"super admin","super admin","+91","+++","superadmin@cargosetu","superadmin",roles);
            staffRepository.save(staff);
            log.info("SuperAdmin has been saved successfully");
        }
    }

    public Role seedDefaultRole(){
        Optional<Role> roleByCodeSuperAdmin = rolesRepo.findByCode(SuperAdminConstants.SUPERADMIN);
        if(roleByCodeSuperAdmin.isEmpty()){
            Role role = new Role(SuperAdminConstants.SUPERADMIN,"Super admin","this is super admin role","PLATFORM");
            role = rolesRepo.save(role);
            log.info("SuperAdmin Role has been saved successfully");
            return role;
        }else return roleByCodeSuperAdmin.get();
    }
}
