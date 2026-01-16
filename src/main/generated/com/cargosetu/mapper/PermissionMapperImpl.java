package com.cargosetu.mapper;

import com.cargosetu.dto.PermissionDTO;
import com.cargosetu.model.Permission;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-16T12:01:30+0530",
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
}
