package com.cargosetu.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.customer.dtos.ChargesDTO;
import com.cargosetu.customer.entity.Charges;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class,uses =  {ExtraChargesMapper.class})
public interface ChargesMapper extends BaseUpdateMapper<ChargesDTO, Charges> {

}
