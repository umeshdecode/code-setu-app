package com.cargosetu.loadmatch.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.loadmatch.dtos.TripShipmentDTO;
import com.cargosetu.loadmatch.entity.TripShipment;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = GlobalMapperConfig.class)
public interface TripShipmentMapper extends BaseUpdateMapper<TripShipmentDTO, TripShipment> {

    @Override
    @Mapping(target = "tripId", source = "trip.tripId")
    @Mapping(target = "shipmentId", source = "shipment.shipmentId")
    @Mapping(target = "loadMatchId", source = "loadMatch.loadMatchId")
    TripShipmentDTO toDto(TripShipment entity);

    @Override
    @Mapping(target = "trip.tripId", source = "tripId")
    @Mapping(target = "shipment.shipmentId", source = "shipmentId")
    @Mapping(target = "loadMatch.loadMatchId", source = "loadMatchId")
    TripShipment toEntity(TripShipmentDTO dto);

    /**
     * Flattens the shipment route/cargo summary fields onto the DTO, same
     * null-safe @AfterMapping technique as LoadMatchMapper.
     */
    @AfterMapping
    default void fillSummaryFields(TripShipment entity, @MappingTarget TripShipmentDTO dto) {
        if (entity.getShipment() != null) {
            dto.setShipmentSourceCity(entity.getShipment().getSourceCity());
            dto.setShipmentDestinationCity(entity.getShipment().getDestinationCity());
            dto.setShipmentCargoType(entity.getShipment().getCargoType());
        }
    }
}
