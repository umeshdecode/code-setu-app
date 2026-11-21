package com.cargosetu.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.customer.dtos.ShiftingDTO;
import com.cargosetu.customer.entity.Shifting;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(config = GlobalMapperConfig.class, uses = {ChargesMapper.class, CustomerMapper.class})
public interface ShiftingMapper extends BaseUpdateMapper<ShiftingDTO, Shifting> {

    @Override
    @Mapping(target = "customer.customerId", source = "customerId")
    @Mapping(target = "charge", source = "chargesDTO")
    Shifting toEntity(ShiftingDTO dto);

    @Override
    @Mapping(target = "customerId", source = "customer.customerId")
    @Mapping(target = "chargesDTO", source = "charge")
    @Mapping(target = "chargesDTODuplicate", ignore = true)
    @Mapping(target = "customerName", source = "customer", qualifiedByName = "customerFullName")
    @Mapping(target = "customerMobileNo", source = "customer.mobileNo")
    @Mapping(target = "customerEmail", source = "customer.email")
    ShiftingDTO toDto(Shifting entity);

    @Named("customerFullName")
    default String customerFullName(com.cargosetu.customer.entity.Customer customer) {
        if (customer == null) return null;
        String first = customer.getFirstName() != null ? customer.getFirstName() : "";
        String last = customer.getLastName() != null ? customer.getLastName() : "";
        return (first + " " + last).trim();
    }
}
