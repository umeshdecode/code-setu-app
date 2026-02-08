package com.cargosetu.customer.service;

import com.cargosetu.customer.dtos.CustomerDTO;
import com.cargosetu.customer.entity.Customer;
import com.cargosetu.dto.ValidationResponse;
import com.cargosetu.mapper.CustomerMapper;
import com.cargosetu.utils.CommonStatusCode;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class CustomerService {

    private final CustomerMapper customerMapper;

    public CustomerService(CustomerMapper customerMapper) {
        this.customerMapper = customerMapper;
    }

    public ValidationResponse validateCustomerCreateRequest(CustomerDTO customerDTO) {
        ValidationResponse validationResponse = new ValidationResponse(true, CommonStatusCode.MSG_VALIDATION_SUCCESS);
        if(Objects.isNull(customerDTO)) return  new ValidationResponse(false, "Invalid Request!!");
        if(customerDTO.getFirstName() == null) return new ValidationResponse(false, "customer name is required!!");

        return validationResponse;
    }

    public void createCustomer(CustomerDTO customerDTO) {
        Customer customer = new Customer();
        customer = customerMapper.toEntity(customerDTO);
        System.out.println(customer);

    }
}
