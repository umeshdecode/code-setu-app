package com.cargosetu.marketplace.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.marketplace.entity.MarketplaceListing;
import org.mapstruct.Mapper;

/**
 * Simple mapper for MarketplaceListing.
 * Listing details are primarily accessed through LoadPostMapper/TruckPostMapper.
 */
@Mapper(config = GlobalMapperConfig.class, componentModel = "spring")
public interface ListingMapper {
    // No BaseUpdateMapper extension — listing is mapped through child post mappers
}
