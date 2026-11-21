package com.cargosetu.marketplace.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import com.cargosetu.marketplace.dto.CreateOfferRequest;
import com.cargosetu.marketplace.dto.OfferResponse;
import com.cargosetu.marketplace.entity.MarketplaceOffer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface OfferMapper extends BaseUpdateMapper<OfferResponse, MarketplaceOffer> {
    @Mapping(target = "listingId", source = "listing.id")
    @Mapping(target = "listingNumber", source = "listing.listingNumber")
    OfferResponse toResponse(MarketplaceOffer entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "listing", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "bidderCompanyId", ignore = true)
    @Mapping(target = "offeredByStaffId", ignore = true)
    @Mapping(target = "parentOfferId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    MarketplaceOffer toEntity(CreateOfferRequest request);
}
