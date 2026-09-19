package com.cargosetu.loadmatch.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.loadmatch.dtos.ShipmentDTO;
import com.cargosetu.loadmatch.entity.Shipment;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class, uses = {ShipmentItemMapper.class})
public interface ShipmentMapper extends BaseUpdateMapper<ShipmentDTO, Shipment> {

    @Override
    @Mapping(target = "customerId", source = "customer.customerId")
    ShipmentDTO toDto(Shipment entity);

    @Override
    @Mapping(target = "customer.customerId", source = "customerId")
    Shipment toEntity(ShipmentDTO dto);
}
