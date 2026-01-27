package com.cargosetu.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.dto.PermissionDTO;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import com.cargosetu.model.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface PermissionMapper extends BaseUpdateMapper<PermissionDTO, Permission> {

    @Mapping(source = "id",target = "permissionId")
    PermissionDTO toDto(Permission permission);

    @Mapping(source = "permissionId",target = "id")
    Permission toEntity(PermissionDTO dto);
}
