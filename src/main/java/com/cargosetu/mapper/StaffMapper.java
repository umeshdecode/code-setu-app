package com.cargosetu.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.dto.StaffDTO;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import com.cargosetu.model.Staff;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class,uses = {RoleMapper.class,DepartmentMapper.class})
public interface StaffMapper extends BaseUpdateMapper<StaffDTO,Staff> {

    @Mapping(source = "password", target = "password",ignore = true)
    StaffDTO toDto(Staff entity);
}
