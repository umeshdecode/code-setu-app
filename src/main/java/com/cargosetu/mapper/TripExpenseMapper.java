package com.cargosetu.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.dto.TripExpenseDTO;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import com.cargosetu.model.TripExpense;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface TripExpenseMapper extends BaseUpdateMapper<TripExpenseDTO, TripExpense> {

    @Override
    @Mapping(target = "tripId", source = "trip.tripId")
    TripExpenseDTO toDto(TripExpense entity);

    @Override
    @Mapping(target = "trip.tripId", source = "tripId")
    TripExpense toEntity(TripExpenseDTO dto);
}
