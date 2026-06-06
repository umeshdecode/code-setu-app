package com.cargosetu.shifting.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.shifting.dtos.ShiftInventoryItemDTO;
import com.cargosetu.shifting.entity.ShiftInventoryItem;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class, uses = {ShiftInventoryPhotoMapper.class, ShiftInventoryPackagingMapper.class})
public interface ShiftInventoryItemMapper extends BaseUpdateMapper<ShiftInventoryItemDTO, ShiftInventoryItem> {

    @Override
    @Mapping(source = "shifting.shiftingId", target = "shiftId")
    @Mapping(source = "shiftInventoryPhotos", target = "photos") // Assuming entity has a collection named shiftInventoryPhotos
    @Mapping(source = "shiftInventoryPackagings", target = "packagingDetails") // Assuming entity has a collection named shiftInventoryPackagings
    ShiftInventoryItemDTO toDto(ShiftInventoryItem entity);

    @Override
    @Mapping(source = "shiftId", target = "shifting.shiftingId")
    @Mapping(source = "photos", target = "shiftInventoryPhotos")
    @Mapping(source = "packagingDetails", target = "shiftInventoryPackagings")
    ShiftInventoryItem toEntity(ShiftInventoryItemDTO dto);
}
