package com.cargosetu.marketplace.mapper;

import java.util.List;
import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.marketplace.dto.RatingRequest;
import com.cargosetu.marketplace.dto.RatingResponse;
import com.cargosetu.marketplace.entity.RatingReview;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface RatingReviewMapper {
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "reviewerCompanyName", ignore = true)
    @Mapping(target = "reviewerName", ignore = true)
    @Mapping(target = "revieweeCompanyName", ignore = true)
    RatingResponse toResponse(RatingReview entity);

    default List<RatingResponse> toResponseList(List<RatingReview> entities) {
        if (entities == null) return null;
        return entities.stream().map(this::toResponse).toList();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "bookingId", ignore = true)
    @Mapping(target = "reviewerCompanyId", ignore = true)
    @Mapping(target = "reviewerStaffId", ignore = true)
    @Mapping(target = "revieweeCompanyId", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    RatingReview toEntity(RatingRequest request);
}
