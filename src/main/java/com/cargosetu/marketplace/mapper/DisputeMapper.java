package com.cargosetu.marketplace.mapper;

import java.util.List;
import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.marketplace.dto.DisputeRequest;
import com.cargosetu.marketplace.dto.DisputeResponse;
import com.cargosetu.marketplace.entity.Dispute;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface DisputeMapper {
    DisputeResponse toResponse(Dispute entity);

    default List<DisputeResponse> toResponseList(List<Dispute> entities) {
        if (entities == null) return null;
        return entities.stream().map(this::toResponse).toList();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "bookingId", ignore = true)
    @Mapping(target = "raisedByStaffId", ignore = true)
    @Mapping(target = "raisedByCompanyId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "resolution", ignore = true)
    @Mapping(target = "resolvedByStaffId", ignore = true)
    @Mapping(target = "resolvedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Dispute toEntity(DisputeRequest request);
}
