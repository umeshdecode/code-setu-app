package com.cargosetu.mapper;

import com.cargosetu.config.GlobalMapperConfig;
import com.cargosetu.customer.dtos.CustomerDTO;
import com.cargosetu.customer.entity.Customer;
import com.cargosetu.mapper.base.BaseUpdateMapper;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class,uses = ShiftingMapper.class)
public interface CustomerMapper extends BaseUpdateMapper<CustomerDTO, Customer> {

}
