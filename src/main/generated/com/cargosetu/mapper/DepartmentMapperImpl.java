package com.cargosetu.mapper;

import com.cargosetu.dto.DepartmentDTO;
import com.cargosetu.model.Department;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-20T11:30:45+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.17 (Microsoft)"
)
@Component
public class DepartmentMapperImpl implements DepartmentMapper {

    @Override
    public Department toEntity(DepartmentDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Department department = new Department();

        department.setIsActive( dto.getIsActive() );
        department.setIsDeleted( dto.getIsDeleted() );
        department.setCreatedBy( dto.getCreatedBy() );
        department.setCreatedByName( dto.getCreatedByName() );
        department.setCreatedAt( dto.getCreatedAt() );
        department.setUpdatedAt( dto.getUpdatedAt() );
        department.setDepartmentId( dto.getDepartmentId() );
        department.setDepartmentUuid( dto.getDepartmentUuid() );
        department.setCompanyUuid( dto.getCompanyUuid() );
        department.setCode( dto.getCode() );
        department.setName( dto.getName() );
        department.setDescription( dto.getDescription() );

        return department;
    }

    @Override
    public DepartmentDTO toDto(Department entity) {
        if ( entity == null ) {
            return null;
        }

        DepartmentDTO departmentDTO = new DepartmentDTO();

        departmentDTO.setDepartmentId( entity.getDepartmentId() );
        departmentDTO.setDepartmentUuid( entity.getDepartmentUuid() );
        departmentDTO.setCompanyUuid( entity.getCompanyUuid() );
        departmentDTO.setCode( entity.getCode() );
        departmentDTO.setName( entity.getName() );
        departmentDTO.setDescription( entity.getDescription() );
        departmentDTO.setIsActive( entity.getIsActive() );
        departmentDTO.setIsDeleted( entity.getIsDeleted() );
        departmentDTO.setCreatedByName( entity.getCreatedByName() );
        departmentDTO.setCreatedBy( entity.getCreatedBy() );
        departmentDTO.setCreatedAt( entity.getCreatedAt() );
        departmentDTO.setUpdatedAt( entity.getUpdatedAt() );

        return departmentDTO;
    }

    @Override
    public void updateFromDto(DepartmentDTO dto, Department entity) {
        if ( dto == null ) {
            return;
        }

        if ( dto.getIsActive() != null ) {
            entity.setIsActive( dto.getIsActive() );
        }
        if ( dto.getIsDeleted() != null ) {
            entity.setIsDeleted( dto.getIsDeleted() );
        }
        if ( dto.getCreatedBy() != null ) {
            entity.setCreatedBy( dto.getCreatedBy() );
        }
        if ( dto.getCreatedByName() != null ) {
            entity.setCreatedByName( dto.getCreatedByName() );
        }
        if ( dto.getCreatedAt() != null ) {
            entity.setCreatedAt( dto.getCreatedAt() );
        }
        if ( dto.getUpdatedAt() != null ) {
            entity.setUpdatedAt( dto.getUpdatedAt() );
        }
        if ( dto.getDepartmentId() != null ) {
            entity.setDepartmentId( dto.getDepartmentId() );
        }
        if ( dto.getDepartmentUuid() != null ) {
            entity.setDepartmentUuid( dto.getDepartmentUuid() );
        }
        if ( dto.getCompanyUuid() != null ) {
            entity.setCompanyUuid( dto.getCompanyUuid() );
        }
        if ( dto.getCode() != null ) {
            entity.setCode( dto.getCode() );
        }
        if ( dto.getName() != null ) {
            entity.setName( dto.getName() );
        }
        if ( dto.getDescription() != null ) {
            entity.setDescription( dto.getDescription() );
        }
    }
}
