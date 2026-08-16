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

import static com.cargosetu.utils.PermissionConstants.COMPANY_UPDATE;

@Configuration
@Slf4j
public class SeedSuperAdmin  {

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


//    @Override
    public void run(ApplicationArguments args) throws Exception {
        Role superAdminRole = rolesRepo.findByCode(SuperAdminConstants.SUPERADMIN)
                .orElseGet(this::seedDefaultRole);

        // Synchronize all existing permissions in DB to SuperAdmin role dynamically
        List<Permission> allPermissions = permissionRepo.findAllByIsDeletedIsFalse();
        if (superAdminRole.getPermissions() == null) {
            superAdminRole.setPermissions(new HashSet<>());
        }
        if (!allPermissions.isEmpty()) {
            superAdminRole.getPermissions().addAll(allPermissions);
            superAdminRole = rolesRepo.save(superAdminRole);
        }

        Optional<Staff> superAdminOpt = staffRepository.findByUserName(SuperAdminConstants.SUPERADMIN);
        if (superAdminOpt.isEmpty()) {
            HashSet<Role> roles = new HashSet<>();
            roles.add(superAdminRole);
            Staff staff = new Staff("31201SUPERADMIN31201", SuperAdminConstants.SUPERADMIN, "super admin", "super admin", "+91", "+++", "superadmin@cargosetu", "$2a$10$c6yl2E/iNR99hiHKlg6zkOTJJAD48lhDxQzG5/3ByTMrdg/7XGrbm", roles);
            staff.setStaffType("SUPERADMIN");
            staffRepository.save(staff);
            log.info("SuperAdmin user created and assigned SuperAdmin role successfully");
        }
    }

    public Role seedDefaultRole() {
        Optional<Role> roleByCodeSuperAdmin = rolesRepo.findByCode(SuperAdminConstants.SUPERADMIN);
        if (roleByCodeSuperAdmin.isEmpty()) {
            Role role = new Role(SuperAdminConstants.SUPERADMIN, "Super admin", "this is super admin role", "PLATFORM", false);

            List<Permission> permissions = new ArrayList<>();
            // Customer
            permissions.add(createPermission("CUSTOMER_READ", "Customer Read", "Permission to view customers", "Customer Module"));
            permissions.add(createPermission("CUSTOMER_CREATE", "Customer Create", "Permission to create customers", "Customer Module"));
            permissions.add(createPermission("CUSTOMER_MODIFY", "Customer Modify", "Permission to update customers", "Customer Module"));
            permissions.add(createPermission("CUSTOMER_DELETE", "Customer Delete", "Permission to delete customers", "Customer Module"));
            permissions.add(createPermission("CUSTOMER_ALL", "Customer Full Access", "Full access to customer module", "Customer Module"));

            // Staff
            permissions.add(createPermission("STAFF_READ", "Staff Read", "Permission to view staff", "Staff Module"));
            permissions.add(createPermission("STAFF_CREATE", "Staff Create", "Permission to create staff", "Staff Module"));
            permissions.add(createPermission("STAFF_MODIFY", "Staff Modify", "Permission to update staff", "Staff Module"));
            permissions.add(createPermission("STAFF_DELETE", "Staff Delete", "Permission to delete staff", "Staff Module"));
            permissions.add(createPermission("STAFF_ALL", "Staff Full Access", "Full access to staff module", "Staff Module"));

            // Role
            permissions.add(createPermission("ROLE_READ", "Role Read", "Permission to view roles", "Role Module"));
            permissions.add(createPermission("ROLE_CREATE", "Role Create", "Permission to create roles", "Role Module"));
            permissions.add(createPermission("ROLE_MODIFY", "Role Modify", "Permission to update roles", "Role Module"));
            permissions.add(createPermission("ROLE_DELETE", "Role Delete", "Permission to delete roles", "Role Module"));
            permissions.add(createPermission("ROLE_ALL", "Role Full Access", "Full access to role module", "Role Module"));

            // Department
            permissions.add(createPermission("DEPARTMENT_READ", "Department Read", "Permission to view department", "Department Module"));
            permissions.add(createPermission("DEPARTMENT_CREATE", "Department Create", "Permission to create department", "Department Module"));
            permissions.add(createPermission("DEPARTMENT_MODIFY", "Department Modify", "Permission to update department", "Department Module"));
            permissions.add(createPermission("DEPARTMENT_DELETE", "Department Delete", "Permission to delete department", "Department Module"));
            permissions.add(createPermission("DEPARTMENT_ALL", "Department Full Access", "Full access to department module", "Department Module"));

            // Shifting
            permissions.add(createPermission("SHIFT_READ", "Shift Read", "Permission to view shifting", "Shifting Module"));
            permissions.add(createPermission("SHIFT_CREATE", "Shift Create", "Permission to create shifting", "Shifting Module"));
            permissions.add(createPermission("SHIFT_MODIFY", "Shift Modify", "Permission to update shifting", "Shifting Module"));
            permissions.add(createPermission("SHIFT_DELETE", "Shift Delete", "Permission to delete shifting", "Shifting Module"));
            permissions.add(createPermission("SHIFT_ALL", "Shift Full Access", "Full access to shifting module", "Shifting Module"));

            // Trip
            permissions.add(createPermission("TRIP_READ", "Trip Read", "Permission to view trips", "Trip Module"));
            permissions.add(createPermission("TRIP_CREATE", "Trip Create", "Permission to create trips", "Trip Module"));
            permissions.add(createPermission("TRIP_MODIFY", "Trip Modify", "Permission to update trips", "Trip Module"));
            permissions.add(createPermission("TRIP_DELETE", "Trip Delete", "Permission to delete trips", "Trip Module"));
            permissions.add(createPermission("TRIP_ALL", "Trip Full Access", "Full access to trip module", "Trip Module"));

            // Company
            permissions.add(createPermission("COMPANY_READ", "Company Read", "Permission to view company", "Company Module"));
            permissions.add(createPermission("COMPANY_CREATE", "Company Create", "Permission to create company", "Company Module"));
            permissions.add(createPermission("COMPANY_MODIFY", "Company Modify", "Permission to update company", "Company Module"));
            permissions.add(createPermission("COMPANY_DELETE", "Company Delete", "Permission to delete company", "Company Module"));
            permissions.add(createPermission("COMPANY_ALL", "Company Full Access", "Full access to company module", "Company Module"));

            // Plan
            permissions.add(createPermission("PLAN_READ", "Plan Read", "Permission to view plans", "Plan Module"));
            permissions.add(createPermission("PLAN_CREATE", "Plan Create", "Permission to create plans", "Plan Module"));
            permissions.add(createPermission("PLAN_MODIFY", "Plan Modify", "Permission to update plans", "Plan Module"));
            permissions.add(createPermission("PLAN_DELETE", "Plan Delete", "Permission to delete plans", "Plan Module"));
            permissions.add(createPermission("PLAN_ALL", "Plan Full Access", "Full access to plan module", "Plan Module"));

            Set<Permission> defaultPermissions = createDefaultPermissions(permissions);
            role.setPermissions(defaultPermissions);
            role = rolesRepo.save(role);
            log.info("SuperAdmin Role has been saved successfully");
            return role;
        } else {
            return roleByCodeSuperAdmin.get();
        }
    }

    public Set<Permission> createDefaultPermissions(List<Permission> permissions) {
        List<Permission> result = new ArrayList<>();
        for (Permission perm : permissions) {
            Optional<Permission> existing = permissionRepo.findByCodeAndIsDeletedIsFalse(perm.getCode());
            if (existing.isPresent()) {
                result.add(existing.get());
            } else {
                result.add(permissionRepo.save(perm));
            }
        }
        return new HashSet<>(result);
    }

    public Permission createPermission(String code, String permissionName, String description, String module) {
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
