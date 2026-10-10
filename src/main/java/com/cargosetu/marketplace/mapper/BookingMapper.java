package com.cargosetu.marketplace.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import com.cargosetu.marketplace.dto.CreateBookingRequest;
import com.cargosetu.marketplace.dto.BookingResponse;
import com.cargosetu.marketplace.entity.MarketplaceBooking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface BookingMapper extends BaseUpdateMapper<BookingResponse, MarketplaceBooking> {
    @Mapping(target = "listingId", source = "listing.id")
    @Mapping(target = "listingNumber", source = "listing.listingNumber")
    @Mapping(target = "listingType", source = "listing.type")
    BookingResponse toResponse(MarketplaceBooking entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "listing", ignore = true)
    @Mapping(target = "bookingNumber", ignore = true)
    @Mapping(target = "shipperCompanyId", ignore = true)
    @Mapping(target = "transporterCompanyId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "bookedAt", ignore = true)
    @Mapping(target = "cancellationPolicySnapshot", ignore = true)
    @Mapping(target = "commercialSnapshot", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    MarketplaceBooking toEntity(CreateBookingRequest request);
}
