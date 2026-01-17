package com.cargosetu.service.impl;

import com.cargosetu.dto.DepartmentDTO;
import com.cargosetu.mapper.DepartmentMapper;
import com.cargosetu.model.Department;
import com.cargosetu.model.Permission;
import com.cargosetu.repo.DepartmentRepo;
import com.cargosetu.service.DepartmentService;
import com.cargosetu.utils.ApplicationUtils;
import com.cargosetu.utils.GenericDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


@Slf4j
@Service
public class DepartmentImpl implements DepartmentService {


    private final DepartmentRepo departmentRepo;
    private final DepartmentMapper departmentMapper;

    public DepartmentImpl(DepartmentRepo departmentRepo, DepartmentMapper departmentMapper) {
        this.departmentRepo = departmentRepo;
        this.departmentMapper = departmentMapper;
    }

    @Override
    public GenericDataDTO getDepartmentsList() {
        GenericDataDTO responseDTO = new GenericDataDTO();
        try {
            List<Department> departments = departmentRepo.findAll();
            responseDTO.setResponseMessege("Success");
            responseDTO.setResponseCode(HttpStatus.OK.value());
            responseDTO.setDataList(departments);
        } catch (Exception e) {
            responseDTO.setResponseMessege("Failed to fetch departments");
        }
        return responseDTO;
    }

    @Override
    public GenericDataDTO getDepartmentById(Integer id) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            log.info("Fetching department with ID: {}", id);

            Optional<Department> department = departmentRepo.findById(id);

            if (department.isPresent() && !department.get().getIsDeleted()) {
                response.setResponseMessege("department found");
                response.setResponseCode(200);
                response.setData(department.get());
            } else {
                response.setResponseMessege("department not found");
                response.setResponseCode(404);
                log.info("department not found for ID: {}", id);
            }
        } catch (Exception e) {
            log.error("Error while fetching department with ID: {} ",id, e);
            response.setResponseMessege("Failed to fetch department");
            response.setResponseCode(500);
        }
        return response;
    }


    @Override
    public GenericDataDTO updateDepartment(Integer id, DepartmentDTO pojo) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (pojo == null) {
                response.setResponseMessege("Request body cannot be null");
                response.setResponseCode(400);
                return response;
            }
            log.info("Updating department with ID: {}", id);

            Optional<Department> optionalDepartment = departmentRepo.findById(id);

            if (optionalDepartment.isPresent()) {
                Department department = optionalDepartment.get();

                if(!department.getCode().equals(pojo.getCode())) {
                    Optional<Department> departmentOptional = departmentRepo.findByCodeAndIsDeletedIsFalse(pojo.getCode());
                    if (departmentOptional.isPresent()) {
                        if(!Objects.equals(departmentOptional.get().getDepartmentId(), id)){
                            response.setResponseCode(HttpStatus.CONFLICT.value());
                            response.setResponseMessege("please use different Department code,this code is already present");
                            return response;
                        }
                    }
                }

                departmentMapper.updateFromDto(pojo, department);
                department.setUpdatedAt(LocalDateTime.now());
                departmentRepo.save(department);


                response.setResponseMessege("department updated successfully");
                response.setResponseCode(200);
                response.setData(department);

                log.info("department updated successfully for ID: {}", id);
            } else {
                response.setResponseMessege("department not found");
                response.setResponseCode(404);
                log.info("Update failed. department not found for ID: {}", id);
            }
        } catch (Exception e) {
            log.error("Error while updating department with ID: {}", id, e);
            response.setResponseMessege("Failed to update department");
            response.setResponseCode(500);
        }
        return response;
    }


    @Override
    public GenericDataDTO deleteDepartment(Integer id) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            log.info("Deleting department with ID: {}", id);

            Optional<Department> optionalDepartment = departmentRepo.findById(id);

            if (optionalDepartment.isPresent()) {
                Department department = optionalDepartment.get();
                department.setIsDeleted(true);
                department.setIsActive(false);
                department.setUpdatedAt(LocalDateTime.now());

                departmentRepo.save(department);

                response.setResponseMessege("department deleted successfully");
                response.setResponseCode(200);

                log.info("department soft-deleted successfully for ID: {}", id);
            } else {
                response.setResponseMessege("department not found");
                response.setResponseCode(404);
                log.info("Delete failed. department not found for ID: {}", id);
            }
        } catch (Exception e) {
            log.info("Error while deleting department with ID: {}", id, e);
            response.setResponseMessege("Failed to delete department");
            response.setResponseCode(500);
        }
        return response;
    }


    @Override
    public GenericDataDTO createDepartment(DepartmentDTO pojo) {
        GenericDataDTO response = new GenericDataDTO();
        try {

            boolean departmentnExistsByCode = departmentRepo.existsByCode(pojo.getCode());
            if (departmentnExistsByCode) {
                response.setResponseMessege("Department already exists, cannot create duplicate!");
                response.setResponseCode(HttpStatus.CONFLICT.value());
                response.setData(null);
                return response;
            }

            log.info("Creating new department with name: {} ", pojo.getName());

            Department department = new Department();
            department.setName(pojo.getName());
            department.setCode(pojo.getCode());
            department.setDescription(pojo.getDescription());
            department.setDepartmentUuid(pojo.getDepartmentUuid());
            department.setCompanyUuid(pojo.getCompanyUuid());
            department.setIsActive(true);
            department.setCreatedBy(Long.parseLong(Objects.requireNonNull(ApplicationUtils.getCurrentUser()).getUserId().toString()));
            department = departmentRepo.save(department);

            response.setResponseMessege("department Created Successfully");
            response.setResponseCode(201);
            response.setData(department);

            log.info("department created successfully with ID: {} ", department.getDepartmentId());
        } catch (Exception e) {
            log.info("Error while creating department ", e);
            response.setResponseMessege("Failed to create department");
            response.setResponseCode(500);
        }
        return response;
    }
}
