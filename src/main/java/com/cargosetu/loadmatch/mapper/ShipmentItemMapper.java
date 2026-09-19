package com.cargosetu.loadmatch.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.loadmatch.dtos.ShipmentItemDTO;
import com.cargosetu.loadmatch.entity.ShipmentItem;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface ShipmentItemMapper extends BaseUpdateMapper<ShipmentItemDTO, ShipmentItem> {

    @Override
    @Mapping(target = "shipmentId", source = "shipment.shipmentId")
    ShipmentItemDTO toDto(ShipmentItem entity);

    @Override
    @Mapping(target = "shipment.shipmentId", source = "shipmentId")
    ShipmentItem toEntity(ShipmentItemDTO dto);
}
