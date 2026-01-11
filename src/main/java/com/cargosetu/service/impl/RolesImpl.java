package com.cargosetu.service.impl;

import com.cargosetu.dto.RolesDTO;
import com.cargosetu.model.Role;
import com.cargosetu.repo.RolesRepo;
import com.cargosetu.service.RolesService;
import com.cargosetu.utils.GenericDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class RolesImpl implements RolesService {
    @Autowired
    private RolesRepo rolesRepo;

//    private static final Logger logger = LoggerFactory.getLogger(RolesImpl.class);

    @Override
    public GenericDataDTO getRolseList() {
        GenericDataDTO responseDTO = new GenericDataDTO();
        try {
            List<Role> roles = rolesRepo.findAll();

            responseDTO.setResponseMessege("Success");
            responseDTO.setResponseCode(HttpStatus.OK.value());
            responseDTO.setDataList((org.hibernate.mapping.List) roles);   // Correct way
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
                response.setDate(role.get());
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


    public GenericDataDTO updateRole(Long id, RolesDTO pojo) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            log.info("Updating role with ID: {}", id);

            Optional<Role> optionalRole = rolesRepo.findById(id);

            if (optionalRole.isPresent()) {
                Role role = optionalRole.get();
                role.setRoleName(pojo.getRoleName());
                role.setDiscription(pojo.getDiscription());
                role.setCode(pojo.getCode());
                role.setScope(pojo.getScope());
                role.setUpdatedAt(LocalDateTime.now());

                rolesRepo.save(role);

                response.setResponseMessege("Role updated successfully");
                response.setResponseCode(200);
                response.setDate(role);

                log.info("Role updated successfully for ID: {}", id);
            } else {
                response.setResponseMessege("Role not found");
                response.setResponseCode(404);
                log.info("Update failed. Role not found for ID: {}", id);
            }
        } catch (Exception e) {
            log.error("Error while updating role with ID: {}", id, e);
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
            log.info("Creating new role with name: {} ", pojo.getRoleName());

            Role role = new Role();
            role.setRoleName(pojo.getRoleName());
            role.setDiscription(pojo.getDiscription());
            role.setCode(pojo.getCode());
            role.setScope(pojo.getScope());
            role.setIsActive(true);
            role.setIsDeleted(false);
            role.setCreatedAt(LocalDateTime.now());

            role = rolesRepo.save(role);

            response.setResponseMessege("Role Created Successfully");
            response.setResponseCode(201);
            response.setDate(role);

            log.info("Role created successfully with ID: {} ", role.getRolesId());
        } catch (Exception e) {
            log.info("Error while creating role ", e);
            response.setResponseMessege("Failed to create role");
            response.setResponseCode(500);
        }
        return response;
    }

}
