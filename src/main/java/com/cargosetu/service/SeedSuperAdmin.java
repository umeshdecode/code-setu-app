package com.cargosetu.service;

import com.cargosetu.model.Permission;
import com.cargosetu.model.Role;
import com.cargosetu.model.Staff;
import com.cargosetu.repo.PermissionRepo;
import com.cargosetu.repo.RolesRepo;
import com.cargosetu.repo.StaffRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

import java.util.*;

@Configuration
@Slf4j
public class SeedSuperAdmin implements ApplicationRunner {

    private final PermissionRepo permissionRepo;

    public interface SuperAdminConstants {
        String SUPERADMIN = "SUPERADMIN";
    }

    private final StaffRepository staffRepository;
    private final RolesRepo rolesRepo;

    public SeedSuperAdmin(StaffRepository staffRepository, RolesRepo rolesRepo, PermissionRepo permissionRepo) {
        this.staffRepository = staffRepository;
        this.rolesRepo = rolesRepo;
        this.permissionRepo = permissionRepo;
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
            Staff staff = new Staff("31201SUPERADMIN31201",SuperAdminConstants.SUPERADMIN,"super admin","super admin","+91","+++","superadmin@cargosetu","$2a$10$c6yl2E/iNR99hiHKlg6zkOTJJAD48lhDxQzG5/3ByTMrdg/7XGrbm",roles);
            staffRepository.save(staff);
            log.info("SuperAdmin has been saved successfully");
        }
    }

    public Role seedDefaultRole(){
        Optional<Role> roleByCodeSuperAdmin = rolesRepo.findByCode(SuperAdminConstants.SUPERADMIN);
        if(roleByCodeSuperAdmin.isEmpty()){
            Role role = new Role(SuperAdminConstants.SUPERADMIN,"Super admin","this is super admin role","PLATFORM");

            List<Permission> permissions = new ArrayList<>();
            permissions.add(createPermission("ROLE_CREATE","Role Create","permission for create roles","Role Module"));
            permissions.add(createPermission("ROLE_READ","Role READ","permission for read roles","Role Module"));
            permissions.add(createPermission("ROLE_UPDATE_DELETE","Role Update Delete","permission for update/delete roles","Role Module"));
            permissions.add(createPermission("STAFF_CREATE","Staff Create","permission for create staffs","Staff Module"));
            permissions.add(createPermission("STAFF_READ","Staff Read","permission for read staffs","Staff Module"));
            permissions.add(createPermission("STAFF_UPDATE_DELETE","Staff Update Delete","permission for update/delete staffs","Staff Module"));
            permissions.add(createPermission("DEPARTMENT_UPDATE_DELETE","Department Update Delete","permission for update/delete Department","Department Module"));
            permissions.add(createPermission("DEPARTMENT_CREATE","Department create","permission for create Department","Department Module"));
            permissions.add(createPermission("DEPARTMENT_READ","Department read","permission for read Department","Department Module"));
            permissions.add(createPermission("PLAN_UPDATE_DELETE","Plan Update Delete","permission for update/delete Plan","Plan Module"));
            permissions.add(createPermission("PLAN_CREATE","Plan create Delete","permission for create Plan","Plan Module"));
            permissions.add(createPermission("PLAN_READ","Plan read Delete","permission for read Plan","Plan Module"));
            permissions.add(createPermission("COMPANY_CREATE","Company create ","permission for create Company","Company Module"));
            permissions.add(createPermission("COMPANY_UPDATE_DELETE","Company Update/Delete","permission for update/delete Company","Staff Module"));
            permissions.add(createPermission("COMPANY_READ","Company read","permission for read Company","Company Module"));
            permissions.add(createPermission("CUSTOMER_CREATE","customer create","permission for create customer","Customer Module"));
            permissions.add(createPermission("CUSTOMER_UPDATE","customer update","permission for update customer","customer Module"));
            permissions.add(createPermission("CUSTOMER_VIEW","customer read","permission for read customer","customer Module"));
            permissions.add(createPermission("CUSTOMER_DELETE","customer delete","permission for delete customer","customer Module"));

            Set<Permission> defaultPermissions = createDefaultPermissions(permissions);
            role.setPermissions(defaultPermissions);
            role = rolesRepo.save(role);
            log.info("SuperAdmin Role has been saved successfully");
            return role;
        }else return roleByCodeSuperAdmin.get();
    }

    public Set<Permission> createDefaultPermissions(List<Permission> permissions){
        List<Permission> savedPermissions = permissionRepo.saveAllAndFlush(permissions);
        return new HashSet<>(savedPermissions);
    }
    public Permission createPermission(String code,String permissionName,String description,String module){
        Permission permission = new Permission();
        permission.setCode(code);
        permission.setName(permissionName);
        permission.setDescription(description);
        permission.setModule(module);
        permission.setIsActive(true);
        permission.setCreatedByName("SYSTEM-DEFAULT");
        return permission;
    }
}
