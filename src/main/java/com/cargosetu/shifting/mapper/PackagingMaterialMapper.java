package com.cargosetu.shifting.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.customer.entity.PackagingMaterial;
import com.cargosetu.shifting.dtos.PackagingMaterialDTO;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
public interface PackagingMaterialMapper extends BaseUpdateMapper<PackagingMaterialDTO, PackagingMaterial> {
}
