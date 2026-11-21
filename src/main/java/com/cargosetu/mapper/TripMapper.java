package com.cargosetu.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.dto.TripDTO;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import com.cargosetu.model.Trip;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface TripMapper extends BaseUpdateMapper<TripDTO, Trip> {

    @Override
    @Mapping(target = "shiftingId", source = "shifting.shiftingId")
    @Mapping(target = "driverId", source = "driver.id")
    @Mapping(target = "vehicleId", source = "vehicle.id")
    TripDTO toDto(Trip entity);

    @Override
    @Mapping(target = "shifting.shiftingId", source = "shiftingId")
    @Mapping(target = "driver.id", source = "driverId")
    @Mapping(target = "vehicle.id", source = "vehicleId")
    Trip toEntity(TripDTO dto);
}
