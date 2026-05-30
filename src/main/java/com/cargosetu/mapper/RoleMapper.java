package com.cargosetu.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.dto.RolesDTO;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import com.cargosetu.model.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class,uses = PermissionMapper.class)
public interface RoleMapper extends BaseUpdateMapper<RolesDTO, Role> {

    @Mapping(source = "roleId", target = "rolesId")
    Role toEntity(RolesDTO dto);

    @Mapping(source = "rolesId",target = "roleId")
    RolesDTO toDto(Role entity);
}

