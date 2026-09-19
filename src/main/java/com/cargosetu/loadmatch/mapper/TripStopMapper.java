package com.cargosetu.loadmatch.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.loadmatch.dtos.TripStopDTO;
import com.cargosetu.loadmatch.entity.TripStop;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface TripStopMapper extends BaseUpdateMapper<TripStopDTO, TripStop> {

    @Override
    @Mapping(target = "tripId", source = "trip.tripId")
    TripStopDTO toDto(TripStop entity);

    @Override
    @Mapping(target = "trip.tripId", source = "tripId")
    TripStop toEntity(TripStopDTO dto);
}
