package com.cargosetu.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.dto.ClientServiceDTO;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import com.cargosetu.model.Clientservice;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
public interface ClientServiceMapper extends BaseUpdateMapper<ClientServiceDTO, Clientservice> {
}
