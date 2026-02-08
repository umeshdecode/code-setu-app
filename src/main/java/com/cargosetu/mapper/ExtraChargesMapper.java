package com.cargosetu.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.customer.dtos.ExtraChargesDTO;
import com.cargosetu.customer.entity.ExtraCharges;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
public interface ExtraChargesMapper extends BaseUpdateMapper<ExtraChargesDTO, ExtraCharges> {
}
