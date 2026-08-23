package com.cargosetu.customer.controller;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.config.utils.UrlConstants.CustomerUrlConstants;
import com.cargosetu.customer.dtos.CustomerDTO;
import com.cargosetu.customer.dtos.CustomerUpdateDto;
import com.cargosetu.customer.service.CustomerService;
import com.cargosetu.dto.ValidationResponse;
import com.cargosetu.utils.BaseURL;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Slf4j
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
    public GenericDataDTO createCustomer(@AuthenticationPrincipal LoggedInUser user,@RequestBody CustomerDTO customerDTO) {
        GenericDataDTO genericDataDTO = new GenericDataDTO();
        try {
            log.info("POST /customer - createCustomer request received, firstName={}, shiftingsCount={}",
                    customerDTO.getFirstName(),
                    customerDTO.getShiftings() != null ? customerDTO.getShiftings().size() : 0);

            genericDataDTO.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            genericDataDTO.setResponseMessege(CommonStatusCode.MSG_INTERNAL_SERVER_ERROR);
            ValidationResponse validationResponse = customerService.validateCustomerCreateRequest(customerDTO);
            if(validationResponse != null && validationResponse.isValid()){
              genericDataDTO =   customerService.createCustomer(user,customerDTO);
            }else {
                log.warn("Customer create validation failed: {}", validationResponse != null ? validationResponse.getMessage() : "null");
                genericDataDTO.setResponseMessege(validationResponse.getMessage() != null ? validationResponse.getMessage() : CommonStatusCode.MSG_BAD_REQUEST);
                genericDataDTO.setResponseCode(CommonStatusCode.VALIDATION_ERROR);
            }
            return genericDataDTO;
        } catch (Exception e) {
            log.error("Unexpected error in createCustomer: {}", e.getMessage(), e);
        }
        return genericDataDTO;
    }

    @PreAuthorize(
            "hasAuthority(T(com.cargosetu.utils.PermissionConstants).CUSTOMER_UPDATE)"
    )
    @PutMapping
    public GenericDataDTO updateCustomer(@RequestBody CustomerDTO customerDTO) {
        log.info("PUT /customer - updateCustomer request received, customerId={}, firstName={}, shiftingsCount={}",
                customerDTO.getCustomerId(), customerDTO.getFirstName(),
                customerDTO.getShiftings() != null ? customerDTO.getShiftings().size() : 0);

        GenericDataDTO genericDataDTO = new GenericDataDTO();
        genericDataDTO.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
        genericDataDTO.setResponseMessege(CommonStatusCode.MSG_INTERNAL_SERVER_ERROR);
        ValidationResponse isValidForUpdate = customerService.validateCustomerUpdateRequest(customerDTO);
        if(isValidForUpdate != null && isValidForUpdate.isValid()){
            genericDataDTO =   customerService.updateCustomer(customerDTO);
        }else {
            log.warn("Customer update validation failed: {}", isValidForUpdate != null ? isValidForUpdate.getMessage() : "null");
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
        log.info("DELETE /customer - deleteCustomer request received, customerId={}, uuid={}",
                deleteDto.getCustomerId(), deleteDto.getUuid());
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
        log.info("GET /customer/{} - getCustomerData request received", uuid);
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
    public GenericDataDTO getCustomerData(@AuthenticationPrincipal LoggedInUser user, @RequestParam Integer pageNumber, @RequestParam Integer pageSize) {
        log.info("GET /customer - getAllCustomers request received, pageNumber={}, pageSize={}", pageNumber, pageSize);
        GenericDataDTO customerResponse = new GenericDataDTO();
        customerResponse.setResponseCode(CommonStatusCode.FAIL);
        customerResponse.setResponseMessege(CommonStatusCode.MSG_BAD_REQUEST);
        if(pageNumber == null) pageNumber = 0;
        if(pageSize == null) pageSize = 5;
        customerResponse = customerService.getAllCustomer(user,pageNumber,pageSize);

        if (customerResponse.getResponseCode() == null) {
            customerResponse.setResponseCode(CommonStatusCode.FAIL);
            customerResponse.setResponseMessege(CommonStatusCode.MSG_BAD_REQUEST);
        }
        return customerResponse;
    }

}
