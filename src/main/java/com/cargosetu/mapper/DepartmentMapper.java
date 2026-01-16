package com.cargosetu.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.dto.DepartmentDTO;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import com.cargosetu.model.Department;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
public interface DepartmentMapper extends BaseUpdateMapper<DepartmentDTO, Department> {
}
