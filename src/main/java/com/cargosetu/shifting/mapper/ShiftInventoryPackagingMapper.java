package com.cargosetu.shifting.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.shifting.dtos.ShiftInventoryPackagingDTO;
import com.cargosetu.shifting.entity.ShiftInventoryPackaging;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.InheritConfiguration; // Import this

@Mapper(config = GlobalMapperConfig.class, uses = {PackagingMaterialMapper.class})
public interface ShiftInventoryPackagingMapper extends BaseUpdateMapper<ShiftInventoryPackagingDTO, ShiftInventoryPackaging> {

    @Override
    @Mapping(source = "inventoryItem.id", target = "inventoryItemId")
    @Mapping(source = "packagingMaterial.materialId", target = "packagingMaterialId")
    @Mapping(source = "packagingMaterial.materialName", target = "materialName")
    @Mapping(source = "packagingMaterial.unit", target = "unit")
    ShiftInventoryPackagingDTO toDto(ShiftInventoryPackaging entity);

    @Override
    @Mapping(source = "inventoryItemId", target = "inventoryItem.id")
    @Mapping(source = "packagingMaterialId", target = "packagingMaterial.materialId")
    ShiftInventoryPackaging toEntity(ShiftInventoryPackagingDTO dto);

    @Override
    @InheritConfiguration(name = "toEntity")
    void updateFromDto(ShiftInventoryPackagingDTO dto, @MappingTarget ShiftInventoryPackaging entity);
}