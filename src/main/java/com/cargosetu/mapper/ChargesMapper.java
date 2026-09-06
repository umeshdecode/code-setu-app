package com.cargosetu.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.customer.dtos.ChargesDTO;
import com.cargosetu.customer.entity.Charges;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class,uses =  {ExtraChargesMapper.class})
public interface ChargesMapper extends BaseUpdateMapper<ChargesDTO, Charges> {
    @Override
    @Mapping(target = "extraCharges" ,source = "extraChargesList")
    Charges toEntity(ChargesDTO dto);

    @Override
    @Mapping(target = "extraChargesList" ,source = "extraCharges")
    @Mapping(target = "extraChargesListDuplicate", ignore = true)
    ChargesDTO toDto(Charges entity);


}
