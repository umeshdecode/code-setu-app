package com.cargosetu.customer.controller;

import com.cargosetu.config.utils.UrlConstants.CustomerUrlConstants;
import com.cargosetu.customer.dtos.CustomerDTO;
import com.cargosetu.customer.dtos.CustomerUpdateDto;
import com.cargosetu.customer.service.CustomerService;
import com.cargosetu.dto.ValidationResponse;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(BaseURL.BASE_URL + CustomerUrlConstants.CUSTOMER)
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }



    @PreAuthorize(
            "hasAuthority(T(com.cargosetu.utils.PermissionConstants).CUSTOMER_CREATE)"
    )
    @PostMapping
    public GenericDataDTO createCustomer(@RequestBody CustomerDTO customerDTO) {
        GenericDataDTO genericDataDTO = new GenericDataDTO();
        genericDataDTO.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        genericDataDTO.setResponseMessege(CommonStatusCode.MSG_INTERNAL_SERVER_ERROR);
        ValidationResponse validationResponse = customerService.validateCustomerCreateRequest(customerDTO);
        if(validationResponse != null && validationResponse.isValid()){
          genericDataDTO =   customerService.createCustomer(customerDTO);
        }else {
            genericDataDTO.setResponseMessege(validationResponse.getMessage() != null ? validationResponse.getMessage() : CommonStatusCode.MSG_BAD_REQUEST);
            genericDataDTO.setResponseCode(CommonStatusCode.VALIDATION_ERROR);
        }
        return genericDataDTO;
    }

    @PreAuthorize(
            "hasAuthority(T(com.cargosetu.utils.PermissionConstants).CUSTOMER_UPDATE)"
    )
    @PutMapping
    public GenericDataDTO updateCustomer(@RequestBody CustomerUpdateDto customerDTO) {
        GenericDataDTO genericDataDTO = new GenericDataDTO();
        genericDataDTO.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        genericDataDTO.setResponseMessege(CommonStatusCode.MSG_INTERNAL_SERVER_ERROR);
        ValidationResponse isValidForUpdate = customerService.validateCustomerUpdateRequest(customerDTO);
        if(isValidForUpdate != null && isValidForUpdate.isValid()){
            genericDataDTO =   customerService.updateCustomer(customerDTO);
        }else {
            genericDataDTO.setResponseMessege(isValidForUpdate.getMessage() != null ? isValidForUpdate.getMessage() : CommonStatusCode.MSG_BAD_REQUEST);
            genericDataDTO.setResponseCode(CommonStatusCode.VALIDATION_ERROR);
        }
        return genericDataDTO;
    }

    @DeleteMapping
    @PreAuthorize(
            "hasAuthority(T(com.cargosetu.utils.PermissionConstants).CUSTOMER_DELETE)"
    )
    public GenericDataDTO deleteCustomer(@RequestBody CustomerUpdateDto deleteDto) {
        GenericDataDTO deleteResponse = null;
        deleteResponse = customerService.deleteCustomer(deleteDto);
        if(deleteResponse.getResponseCode() == null){
            deleteResponse.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            deleteResponse.setResponseMessege(CommonStatusCode.MSG_INTERNAL_SERVER_ERROR);
        }
        return deleteResponse;
    }

    @GetMapping("/{uuid}")
    @PreAuthorize(
            "hasAuthority(T(com.cargosetu.utils.PermissionConstants).CUSTOMER_VIEW)"
    )
    public GenericDataDTO getCustomerData(@PathVariable("uuid") String uuid) {
        GenericDataDTO customerResponse = new GenericDataDTO();
        customerResponse.setResponseCode(CommonStatusCode.FAIL);
        customerResponse.setResponseMessege(CommonStatusCode.MSG_BAD_REQUEST);
        if(StringUtils.hasText(uuid)){
            customerResponse = customerService.getCustomer(uuid);

            if(customerResponse.getResponseCode() == null){
                customerResponse.setResponseCode(CommonStatusCode.FAIL);
                customerResponse.setResponseMessege(CommonStatusCode.MSG_BAD_REQUEST);
            }
        }
        return customerResponse;
    }
    @GetMapping
    @PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).CUSTOMER_VIEW)")
    public GenericDataDTO getCustomerData() {
        GenericDataDTO customerResponse = new GenericDataDTO();
        customerResponse.setResponseCode(CommonStatusCode.FAIL);
        customerResponse.setResponseMessege(CommonStatusCode.MSG_BAD_REQUEST);
            customerResponse = customerService.getAllCustomer();

            if(customerResponse.getResponseCode() == null){
                customerResponse.setResponseCode(CommonStatusCode.FAIL);
                customerResponse.setResponseMessege(CommonStatusCode.MSG_BAD_REQUEST);
            }
//    @GetMapping("customers")
//    public GenericDataDTO getCustomerByCompanyCode(){
//
//    }

}
