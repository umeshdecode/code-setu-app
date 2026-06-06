package com.cargosetu.shifting.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.shifting.dtos.ShiftInventoryPhotoDTO;
import com.cargosetu.shifting.entity.ShiftInventoryPhoto;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface ShiftInventoryPhotoMapper extends BaseUpdateMapper<ShiftInventoryPhotoDTO, ShiftInventoryPhoto> {

    @Override
    @Mapping(source = "inventoryItem.id", target = "inventoryItemId")
    ShiftInventoryPhotoDTO toDto(ShiftInventoryPhoto entity);

    @Override
    @Mapping(source = "inventoryItemId", target = "inventoryItem.id")
    ShiftInventoryPhoto toEntity(ShiftInventoryPhotoDTO dto);
}
