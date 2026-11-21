package com.cargosetu.marketplace.mapper;

import java.util.List;
import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.marketplace.dto.TripEventRequest;
import com.cargosetu.marketplace.dto.TripEventResponse;
import com.cargosetu.marketplace.entity.TripEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(config = GlobalMapperConfig.class)
public interface TripEventMapper {

    @Named("toResponse")
    TripEventResponse toResponse(TripEvent entity);

    default List<TripEventResponse> toResponseList(List<TripEvent> entities) {
        if (entities == null) return null;
        return entities.stream().map(this::toResponse).toList();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tripId", ignore = true)
    @Mapping(target = "actorStaffId", ignore = true)
    @Mapping(target = "previousStatus", ignore = true)
    @Mapping(target = "eventTime", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    TripEvent toEntity(TripEventRequest request);
}
