package com.cargosetu.customer.controller;

import com.cargosetu.config.utils.UrlConstants.CustomerUrlConstants;
import com.cargosetu.customer.dtos.CustomerDTO;
import com.cargosetu.customer.service.CustomerService;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.GenericDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BaseURL.BASE_URL + CustomerUrlConstants.CUSTOMER)
public class CustomerController {

    @Autowired
    private CustomerService customerService;

}
