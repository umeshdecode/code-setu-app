package com.cargosetu.marketplace.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import com.cargosetu.marketplace.dto.MarketplaceTripResponse;
import com.cargosetu.marketplace.entity.MarketplaceTrip;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
public interface MarketplaceTripMapper extends BaseUpdateMapper<MarketplaceTripResponse, MarketplaceTrip> {
    MarketplaceTripResponse toResponse(MarketplaceTrip entity);
}
