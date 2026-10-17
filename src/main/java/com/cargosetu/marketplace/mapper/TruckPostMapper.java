package com.cargosetu.marketplace.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.marketplace.dto.CreateTruckPostRequest;
import com.cargosetu.marketplace.dto.TruckPostResponse;
import com.cargosetu.marketplace.entity.TruckPost;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface TruckPostMapper {
    @Mapping(target = "listingId", source = "listing.id")
    @Mapping(target = "listingNumber", source = "listing.listingNumber")
    @Mapping(target = "status", source = "listing.status")
    @Mapping(target = "companyId", source = "listing.companyId")
    @Mapping(target = "postedByStaffId", source = "listing.postedByStaffId")
    @Mapping(target = "validFrom", source = "listing.validFrom")
    @Mapping(target = "validUntil", source = "listing.validUntil")
    @Mapping(target = "waypoints", ignore = true)
    @Mapping(target = "acceptedLoadTypes", ignore = true)
    @Mapping(target = "companyName", ignore = true)
    @Mapping(target = "vehicleNumber", ignore = true)
    @Mapping(target = "vehicleType", ignore = true)
    @Mapping(target = "truckType", ignore = true)
    @Mapping(target = "bodyType", ignore = true)
    @Mapping(target = "truckLengthFeet", ignore = true)
    @Mapping(target = "passingCapacityTons", ignore = true)
    @Mapping(target = "driverName", ignore = true)
    @Mapping(target = "driverPhone", ignore = true)
    @Mapping(target = "verificationStatus", ignore = true)
    @Mapping(target = "overallRating", ignore = true)
    @Mapping(target = "completedTrips", ignore = true)
    @Mapping(target = "routeDistanceKm", ignore = true)
    @Mapping(target = "distanceFromOriginKm", ignore = true)
    @Mapping(target = "distanceFromDestinationKm", ignore = true)
    @Mapping(target = "distanceFromCorridorKm", ignore = true)
    @Mapping(target = "distanceType", ignore = true)
    TruckPostResponse toResponse(TruckPost entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "listing", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "waypoints", ignore = true)
    @Mapping(target = "acceptedLoadTypes", ignore = true)
    @Mapping(target = "occupiedCapacity", ignore = true)
    TruckPost toEntity(CreateTruckPostRequest request);
}
