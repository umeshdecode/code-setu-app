package com.cargosetu.marketplace.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import com.cargosetu.marketplace.dto.CancellationResponse;
import com.cargosetu.marketplace.entity.Cancellation;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
public interface CancellationMapper extends BaseUpdateMapper<CancellationResponse, Cancellation> {
    CancellationResponse toResponse(Cancellation entity);
}
