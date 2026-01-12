package com.cargosetu.service.impl;


import com.cargosetu.dto.PermissionDTO;
import com.cargosetu.model.Permission;
import com.cargosetu.repo.PermissionRepo;
import com.cargosetu.service.PermissionService;
import com.cargosetu.utils.GenericDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class PermissionImpl implements PermissionService {

    @Autowired
    private PermissionRepo permissionRepo;


    @Override
    public GenericDataDTO getPermissionList() {
        GenericDataDTO responseDTO = new GenericDataDTO();
        try {
            List<Permission> permissions = permissionRepo.findAll();

            responseDTO.setResponseMessege("Success");
            responseDTO.setResponseCode(HttpStatus.OK.value());
            responseDTO.setDataList(permissions);
        } catch (Exception e) {
            responseDTO.setResponseMessege("Failed to fetch permissions");
        }
        return responseDTO;
    }

    @Override
    public GenericDataDTO getPermissionById(Long id) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            log.info("Fetching permission with ID: {}", id);

            Optional<Permission> permission = permissionRepo.findById(id);

            if (permission.isPresent() && !permission.get().getIsDeleted()) {
                response.setResponseMessege("Permission found");
                response.setResponseCode(200);
                response.setDate(permission.get());
            } else {
                response.setResponseMessege("Permission not found");
                response.setResponseCode(404);
                log.info("Permission not found for ID: {}", id);
            }
        } catch (Exception e) {
            log.error("Error while fetching Permission with ID: {} ",id, e);
            response.setResponseMessege("Failed to fetch Permission");
            response.setResponseCode(500);
        }
        return response;
    }


    @Override
    public GenericDataDTO updatePermission(Long id, PermissionDTO pojo) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            log.info("Updating Permission with ID: {}", id);

            Optional<Permission> optionalPermission = permissionRepo.findById(id);

            if (optionalPermission.isPresent()) {
                Permission permission = optionalPermission.get();
                permission.setName(pojo.getName());
                permission.setModule(pojo.getModule());
                permission.setCode(pojo.getCode());
                permission.setDescription(pojo.getDescription());
                permission.setUpdatedAt(LocalDateTime.now());

                permissionRepo.save(permission);

                response.setResponseMessege("Permission updated successfully");
                response.setResponseCode(200);
                response.setDate(permission);

                log.info("Permission updated successfully for ID: {}", id);
            } else {
                response.setResponseMessege("Permission not found");
                response.setResponseCode(404);
                log.info("Update failed. Permission not found for ID: {}", id);
            }
        } catch (Exception e) {
            log.error("Error while updating Permission with ID: {}", id, e);
            response.setResponseMessege("Failed to update Permission");
            response.setResponseCode(500);
        }
        return response;
    }


    @Override
    public GenericDataDTO deletePermission(Long id) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            log.info("Deleting Permission with ID: {}", id);

            Optional<Permission> optionalPermission = permissionRepo.findById(id);

            if (optionalPermission.isPresent()) {
                Permission permission = optionalPermission.get();
                permission.setIsDeleted(true);
                permission.setIsActive(false);
                permission.setUpdatedAt(LocalDateTime.now());

                permissionRepo.save(permission);

                response.setResponseMessege("Permission deleted successfully");
                response.setResponseCode(200);

                log.info("Permission soft-deleted successfully for ID: {}", id);
            } else {
                response.setResponseMessege("Permission not found");
                response.setResponseCode(404);
                log.info("Delete failed. Permission not found for ID: {}", id);
            }
        } catch (Exception e) {
            log.info("Error while deleting Permission with ID: {}", id, e);
            response.setResponseMessege("Failed to delete Permission");
            response.setResponseCode(500);
        }
        return response;
    }


    @Override
    public GenericDataDTO createPermission(PermissionDTO pojo) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            log.info("Creating new Permission with name: {} ", pojo.getName());

            Permission permission = new Permission();
            permission.setName(pojo.getName());
            permission.setCode(pojo.getCode());
            permission.setDescription(pojo.getDescription());
            permission.setModule(pojo.getModule());
            permission.setIsActive(true);
            permission.setIsDeleted(false);
            permission.setCreatedAt(LocalDateTime.now());

            permission = permissionRepo.save(permission);

            response.setResponseMessege("Permission Created Successfully");
            response.setResponseCode(201);
            response.setDate(permission);

            log.info("Permission created successfully with ID: {} ", permission.getId());
        } catch (Exception e) {
            log.info("Error while creating Permission ", e);
            response.setResponseMessege("Failed to create Permission");
            response.setResponseCode(500);
        }
        return response;
    }
}
