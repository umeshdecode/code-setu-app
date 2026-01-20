package com.cargosetu.mapper;

import com.cargosetu.dto.PermissionDTO;
import com.cargosetu.model.Permission;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-20T11:30:45+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.17 (Microsoft)"
)
@Component
public class PermissionMapperImpl implements PermissionMapper {

    @Override
    public void updateFromDto(PermissionDTO dto, Permission entity) {
        if ( dto == null ) {
            return;
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
        if ( dto.getModule() != null ) {
            entity.setModule( dto.getModule() );
        }
    }

    @Override
    public PermissionDTO toDto(Permission permission) {
        if ( permission == null ) {
            return null;
        }

        PermissionDTO permissionDTO = new PermissionDTO();

        permissionDTO.setPermissionId( permission.getId() );
        permissionDTO.setCode( permission.getCode() );
        permissionDTO.setName( permission.getName() );
        permissionDTO.setDescription( permission.getDescription() );
        permissionDTO.setModule( permission.getModule() );

        return permissionDTO;
    }

    @Override
    public Permission toEntity(PermissionDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Permission permission = new Permission();

        permission.setId( dto.getPermissionId() );
        permission.setCode( dto.getCode() );
        permission.setName( dto.getName() );
        permission.setDescription( dto.getDescription() );
        permission.setModule( dto.getModule() );

        return permission;
    }
}
