package com.cargosetu.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.dto.TripDocumentDTO;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import com.cargosetu.model.TripDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface TripDocumentMapper extends BaseUpdateMapper<TripDocumentDTO, TripDocument> {

    @Override
    @Mapping(target = "tripId", source = "trip.tripId")
    TripDocumentDTO toDto(TripDocument entity);

    @Override
    @Mapping(target = "trip.tripId", source = "tripId")
    TripDocument toEntity(TripDocumentDTO dto);
}
