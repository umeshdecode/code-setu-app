package com.cargosetu.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.dto.PermissionDTO;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import com.cargosetu.model.Permission;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
public interface PermissionMapper extends BaseUpdateMapper<PermissionDTO, Permission> {

}
