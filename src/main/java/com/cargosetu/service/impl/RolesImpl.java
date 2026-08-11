package com.cargosetu.service.impl;

import com.cargosetu.dto.RolesDTO;
import com.cargosetu.model.Permission;
import com.cargosetu.model.Role;
import com.cargosetu.repo.PermissionRepo;
import com.cargosetu.repo.RolesRepo;
import com.cargosetu.service.RolesService;
import com.cargosetu.utils.CommonConstants;
import com.cargosetu.utils.GenericDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class RolesImpl implements RolesService {
    @Autowired
    private RolesRepo rolesRepo;
    @Autowired
    private PermissionRepo permissionRepo;

//    private static final Logger logger = LoggerFactory.getLogger(RolesImpl.class);

    @Override
    public GenericDataDTO getRolseList() {
        GenericDataDTO responseDTO = new GenericDataDTO();
        try {
            List<Role> roles = rolesRepo.findAllByIsDeletedIsFalseAndIsShowIsTrueOrderByRoleNameAscRolesIdDesc();

            responseDTO.setResponseMessege("Success");
            responseDTO.setResponseCode(HttpStatus.OK.value());
            responseDTO.setDataList(roles);   // Correct way
        } catch (Exception e) {
            responseDTO.setResponseMessege("Failed to fetch roles");
        }
        return responseDTO;
    }

    public GenericDataDTO getRoleById(Long id) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            log.info("Fetching role with ID: {}", id);

            Optional<Role> role = rolesRepo.findById(id);

            if (role.isPresent() && !role.get().getIsDeleted()) {
                response.setResponseMessege("Role found");
                response.setResponseCode(200);
                response.setData(role.get());
            } else {
                response.setResponseMessege("Role not found");
                response.setResponseCode(404);
                log.info("Role not found for ID: {}", id);
            }
        } catch (Exception e) {
            log.error("Error while fetching role with ID: {} ",id, e);
            response.setResponseMessege("Failed to fetch role");
            response.setResponseCode(500);
        }
        return response;
    }


    public GenericDataDTO updateRole(RolesDTO pojo) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            log.info("Updating role with ID: {}", pojo.getRoleId());

            Optional<Role> optionalRole = rolesRepo.findById(pojo.getRoleId());

            if (optionalRole.isPresent()) {
                Role role = optionalRole.get();

                if(!role.getCode().equals(pojo.getCode())) {
                    Optional<Role> roleOptional = rolesRepo.findByCodeAndIsDeletedIsFalse(pojo.getCode());
                    if (roleOptional.isPresent()) {
                        if(!Objects.equals(roleOptional.get().getRolesId(), pojo.getRoleId())){
                            response.setResponseCode(HttpStatus.CONFLICT.value());
                            response.setResponseMessege("please use different Role code,this code is already present");
                            return response;
                        }
                    }
                }

                role.setRoleName(pojo.getRoleName());
                role.setDiscription(pojo.getDescription());
                role.setCode(pojo.getCode());
                role.setScope(pojo.getScope());
                role.setUpdatedAt(LocalDateTime.now());

                rolesRepo.save(role);

                response.setResponseMessege("Role updated successfully");
                response.setResponseCode(200);
                response.setData(role);

                log.info("Role updated successfully for ID: {}", pojo.getRoleId());
            } else {
                response.setResponseMessege("Role not found");
                response.setResponseCode(404);
                log.info("Update failed. Role not found for ID: {}", pojo.getRoleId());
            }
        } catch (Exception e) {
            log.error("Error while updating role with ID: {}", pojo.getRoleId(), e);
            response.setResponseMessege("Failed to update role");
            response.setResponseCode(500);
        }
        return response;
    }


    public GenericDataDTO deleteRole(Long id) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            log.info("Deleting role with ID: {}", id);

            Optional<Role> optionalRole = rolesRepo.findById(id);

            if (optionalRole.isPresent()) {
                Role role = optionalRole.get();
                role.setIsDeleted(true);
                role.setIsActive(false);
                role.setUpdatedAt(LocalDateTime.now());

                rolesRepo.save(role);

                response.setResponseMessege("Role deleted successfully");
                response.setResponseCode(200);

                log.info("Role soft-deleted successfully for ID: {}", id);
            } else {
                response.setResponseMessege("Role not found");
                response.setResponseCode(404);
                log.info("Delete failed. Role not found for ID: {}", id);
            }
        } catch (Exception e) {
            log.info("Error while deleting role with ID: {}", id, e);
            response.setResponseMessege("Failed to delete role");
            response.setResponseCode(500);
        }
        return response;
    }


    public GenericDataDTO createRole(RolesDTO pojo) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            boolean roleExistsByCode = rolesRepo.existsByCode(pojo.getCode());
            if (roleExistsByCode) {
                response.setResponseMessege("Role already exists, cannot create duplicate!");
                response.setResponseCode(HttpStatus.CONFLICT.value());
                response.setData(null);
                return response;
            }

            log.info("Creating new role with name: {} ", pojo.getRoleName());

            Role role = new Role();
            role.setRoleName(pojo.getRoleName());
            role.setDiscription(pojo.getDescription());
            role.setCode(pojo.getCode());
            role.setScope(pojo.getScope());
            role.setIsActive(true);
            role.setIsDeleted(false);


            if(pojo.getPermissionIds() != null){
                List<Permission> permissions = permissionRepo.findAllById(pojo.getPermissionIds());
                role.setPermissions(new HashSet<>(permissions));
            }

            role = rolesRepo.save(role);

            response.setResponseMessege("Role Created Successfully");
            response.setResponseCode(201);
            response.setData(role);

            log.info("Role created successfully with ID: {} ", role.getRolesId());
        } catch (Exception e) {
            log.info("Error while creating role ", e);
            response.setResponseMessege("Failed to create role");
            response.setResponseCode(500);
        }
        return response;
    }
    public Role createCompanyAdminDefaultRole(){
        Optional<Role> roleOptional = rolesRepo.findByCode(CommonConstants.RolesConstants.COMPANY_ADMIN);
        if(roleOptional.isPresent()){
            return roleOptional.get();
        }else {
            // for temporary purpose only
            List<Permission> permissions = permissionRepo.findAllByCreatedByName(CommonConstants.SYSTEM_DEFAULT);
            Role role = new Role();
            role.setCode(CommonConstants.RolesConstants.COMPANY_ADMIN);
            role.setRoleName("Company Admin");
            role.setDiscription("Has full access to manage company operations");
            role.setScope("COMPANY");
            role.setIsActive(true);
            role.setIsDeleted(false);
            role.setCreatedAt(LocalDateTime.now());
            role.setCreatedByName("SYSTEM");
            role.setPermissions(new HashSet<>(permissions));
            return rolesRepo.save(role);
        }
    }
}
