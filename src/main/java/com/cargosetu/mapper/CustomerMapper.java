package com.cargosetu.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.customer.dtos.CustomerDTO;
import com.cargosetu.customer.entity.Customer;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = GlobalMapperConfig.class,uses = ShiftingMapper.class)
public interface CustomerMapper extends BaseUpdateMapper<CustomerDTO, Customer> {

    @AfterMapping
    default void calculateCustomerShiftCompleted(Customer entity, @MappingTarget CustomerDTO dto) {
        if (entity != null && entity.getShiftings() != null && !entity.getShiftings().isEmpty()) {
            boolean allCompleted = entity.getShiftings().stream()
                    .allMatch(s -> s != null && s.isShiftCompleted());
            dto.setShiftCompleted(allCompleted);
        } else {
            dto.setShiftCompleted(false);
        }
    }
}
