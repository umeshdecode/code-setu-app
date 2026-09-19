package com.cargosetu.loadmatch.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.loadmatch.dtos.LoadMatchDTO;
import com.cargosetu.loadmatch.entity.LoadMatch;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = GlobalMapperConfig.class)
public interface LoadMatchMapper extends BaseUpdateMapper<LoadMatchDTO, LoadMatch> {

    @Override
    @Mapping(target = "shipmentId", source = "shipment.shipmentId")
    @Mapping(target = "tripId", source = "trip.tripId")
    LoadMatchDTO toDto(LoadMatch entity);

    @Override
    @Mapping(target = "shipment.shipmentId", source = "shipmentId")
    @Mapping(target = "trip.tripId", source = "tripId")
    LoadMatch toEntity(LoadMatchDTO dto);

    /**
     * Flattens the shipment/trip route + vehicle summary fields onto the DTO.
     * Done as an @AfterMapping instead of expression mappings because both the
     * shipment and the trip's vehicle/shifting associations can legitimately be
     * null (e.g. a Trip that only exists to carry matched loads may have no
     * Shifting), and we want to fail gracefully rather than throw.
     */
    @AfterMapping
    default void fillSummaryFields(LoadMatch entity, @MappingTarget LoadMatchDTO dto) {
        if (entity.getShipment() != null) {
            dto.setShipmentSourceCity(entity.getShipment().getSourceCity());
            dto.setShipmentDestinationCity(entity.getShipment().getDestinationCity());
            dto.setShipmentCargoType(entity.getShipment().getCargoType());
        }
        if (entity.getTrip() != null) {
            if (entity.getTrip().getShifting() != null) {
                dto.setTripSourceCity(entity.getTrip().getShifting().getShiftFromCity());
                dto.setTripDestinationCity(entity.getTrip().getShifting().getShiftToCity());
            }
            if (entity.getTrip().getVehicle() != null) {
                dto.setTripVehicleRegistrationNumber(entity.getTrip().getVehicle().getRegistrationNumber());
            }
        }
    }
}
