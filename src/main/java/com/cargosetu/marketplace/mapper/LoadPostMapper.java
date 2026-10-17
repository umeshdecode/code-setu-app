package com.cargosetu.marketplace.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import com.cargosetu.marketplace.dto.CreateLoadPostRequest;
import com.cargosetu.marketplace.dto.LoadPostResponse;
import com.cargosetu.marketplace.entity.LoadPost;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface LoadPostMapper extends BaseUpdateMapper<LoadPostResponse, LoadPost> {
    @Mapping(target = "routeDistanceKm", ignore = true)
    @Mapping(target = "routeDistanceType", ignore = true)
    LoadPostResponse toResponse(LoadPost entity);

    @Mapping(target = "listingId", source = "listing.id")
    @Mapping(target = "listingNumber", source = "listing.listingNumber")
    @Mapping(target = "status", source = "listing.status")
    @Mapping(target = "companyId", source = "listing.companyId")
    @Mapping(target = "postedByStaffId", source = "listing.postedByStaffId")
    @Mapping(target = "validFrom", source = "listing.validFrom")
    @Mapping(target = "validUntil", source = "listing.validUntil")
    @Mapping(target = "routeDistanceKm", ignore = true)
    @Mapping(target = "routeDistanceType", ignore = true)
    LoadPostResponse toResponseWithListing(LoadPost entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "listing", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    LoadPost toEntity(CreateLoadPostRequest request);
}
