package com.cargosetu.customer.controller;

import com.cargosetu.config.utils.UrlConstants.CustomerUrlConstants;
import com.cargosetu.customer.dtos.CustomerDTO;
import com.cargosetu.customer.service.CustomerService;
import com.cargosetu.dto.ValidationResponse;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BaseURL.BASE_URL + CustomerUrlConstants.CUSTOMER)
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @PostMapping
    private GenericDataDTO createCustomer(@RequestBody CustomerDTO customerDTO) {
        GenericDataDTO genericDataDTO = new GenericDataDTO();
        genericDataDTO.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        genericDataDTO.setResponseMessege(CommonStatusCode.MSG_INTERNAL_SERVER_ERROR);
        ValidationResponse validationResponse = customerService.validateCustomerCreateRequest(customerDTO);
        if(validationResponse != null && validationResponse.isValid()){
            customerService.createCustomer(customerDTO);
        }else {
            genericDataDTO.setResponseMessege(validationResponse.getMessage() != null ? validationResponse.getMessage() : CommonStatusCode.MSG_BAD_REQUEST);
            genericDataDTO.setResponseCode(CommonStatusCode.VALIDATION_ERROR);
            return  genericDataDTO;
        }
        return genericDataDTO;
    }

}
