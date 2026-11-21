package com.cargosetu.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.dto.TripStatusHistoryDTO;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import com.cargosetu.model.TripStatusHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface TripStatusHistoryMapper extends BaseUpdateMapper<TripStatusHistoryDTO, TripStatusHistory> {

    @Override
    @Mapping(target = "tripId", source = "trip.tripId")
    TripStatusHistoryDTO toDto(TripStatusHistory entity);

    @Override
    @Mapping(target = "trip.tripId", source = "tripId")
    TripStatusHistory toEntity(TripStatusHistoryDTO dto);
}
