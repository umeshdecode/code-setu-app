package com.cargosetu.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.customer.dtos.ShiftingDTO;
import com.cargosetu.customer.entity.Shifting;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class,uses =  {ChargesMapper.class,CustomerMapper.class})
public interface ShiftingMapper extends BaseUpdateMapper<ShiftingDTO, Shifting> {

    @Override
    @Mapping(target = "customer.customerId", source = "customerId")
    @Mapping(target = "charge", source = "chargesDTO")
    Shifting toEntity(ShiftingDTO dto);

    @Override
    @Mapping(target = "customerId", source = "customer.customerId")
    @Mapping(target = "chargesDTO", source = "charge")
    ShiftingDTO toDto(Shifting entity);
}
