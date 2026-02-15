package com.cargosetu.customer.service;

import com.cargosetu.customer.dtos.CustomerDTO;
import com.cargosetu.customer.dtos.CustomerUpdateDto;
import com.cargosetu.customer.dtos.ShiftingDTO;
import com.cargosetu.customer.entity.Customer;
import com.cargosetu.customer.entity.Shifting;
import com.cargosetu.customer.repository.CustomerRepository;
import com.cargosetu.dto.ValidationResponse;
import com.cargosetu.mapper.CustomerMapper;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import com.cargosetu.utils.UUIDGenerator;
import jakarta.persistence.GeneratedValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class CustomerService {

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private CustomerRepository customerRepository;

    public ValidationResponse validateCustomerCreateRequest(CustomerDTO customerDTO) {
        ValidationResponse validationResponse = new ValidationResponse(true, CommonStatusCode.MSG_VALIDATION_SUCCESS);
        if(Objects.isNull(customerDTO)) return  new ValidationResponse(false, "Invalid Request!!");
        if(customerDTO.getFirstName() == null) return new ValidationResponse(false, "customer name is required!!");

        return validationResponse;
    }

    public GenericDataDTO createCustomer(CustomerDTO customerDTO) {
        GenericDataDTO genericDataDTO = new GenericDataDTO();
        Customer customer = new Customer();
        customer = customerMapper.toEntity(customerDTO);
        String generatedUUID = UUIDGenerator.generate();
        customer.setUuid(generatedUUID);
        customer.setIsDelete(false);
        linkCustomerToShifting(customer.getShiftings(),customer);
        customer = customerRepository.save(customer);

        if(Objects.nonNull(customer.getCustomerId())) {

            genericDataDTO.setResponseMessege("customer created successfully!");
            genericDataDTO.setResponseCode(CommonStatusCode.CREATED);
            genericDataDTO.setData(customer);
        }else  {
            genericDataDTO.setResponseMessege("customer not created!");
            genericDataDTO.setResponseCode(CommonStatusCode.FAIL);
        }
        return genericDataDTO;
    }

    private void linkCustomerToShifting(List<Shifting> shiftings, Customer customer) {
        for(Shifting shifting : shiftings) {
            shifting.setCustomer(customer);
        }
    }


    public ValidationResponse validateCustomerUpdateRequest(CustomerUpdateDto customerDTO) {
        ValidationResponse validationResponse = new ValidationResponse(true, CommonStatusCode.MSG_VALIDATION_SUCCESS);
        if(Objects.isNull(customerDTO)) return  new ValidationResponse(false, "Invalid Request!!");
        if(customerDTO.getFirstName() == null) return new ValidationResponse(false, "customer name is required!!");
        if(customerDTO.getCustomerId() != null){
            int count = customerRepository.countCustomersByCustomerId(customerDTO.getCustomerId());
            if(count <= 0){
                return new ValidationResponse(false, "Invalid customer");
            }
        }
        return validationResponse;
    }

    public GenericDataDTO updateCustomer(CustomerUpdateDto customerDTO) {
        GenericDataDTO updateResponse = new GenericDataDTO();
        try {
            Optional<Customer> dbCust = customerRepository.findByCustomerIdAndIsDelete(customerDTO.getCustomerId(),false);
            if(dbCust.isPresent()) {
                Customer customer = dbCust.get();
                customer.setFirstName(customerDTO.getFirstName());
                customer.setLastName(customerDTO.getLastName());
                customer.setEmail(customerDTO.getEmail());
                customer.setMobileNo(customerDTO.getMobileNo());
                customer.setAlternateMobileNo(customerDTO.getAlternateMobileNo());
                customer.setWorkEmail(customerDTO.getWorkEmail());
                customer = customerRepository.save(customer);
                updateResponse.setData(customer);
                updateResponse.setResponseCode(CommonStatusCode.SUCCESS);
                updateResponse.setResponseMessege("customer updated successfully!");
            }else {
                updateResponse.setResponseCode(CommonStatusCode.FAIL);
                updateResponse.setResponseMessege("something went wrong, try after sometime");
            }
        } catch (Exception e) {
            log.error("error while updating customer");
        }
        return updateResponse;
    }

    public GenericDataDTO deleteCustomer(CustomerUpdateDto deleteDto) {
        GenericDataDTO deleteResponse = new GenericDataDTO();
        if(Objects.isNull(deleteDto.getCustomerId())) {
            deleteResponse.setResponseCode(CommonStatusCode.FAIL);
            deleteResponse.setResponseMessege("something went wrong");
            return deleteResponse;
        }
        Optional<Customer> customer = customerRepository.findByUuidAndIsDelete(deleteDto.getUuid(),false);
        if(customer.isPresent()){
            customer.get().setIsDelete(true);
            customerRepository.save(customer.get());
            deleteResponse.setResponseCode(CommonStatusCode.SUCCESS);
            deleteResponse.setResponseMessege(CommonStatusCode.MSG_DELETED);
        }else {
            deleteResponse.setResponseCode(CommonStatusCode.FAIL);
            deleteResponse.setResponseMessege(CommonStatusCode.MSG_INTERNAL_SERVER_ERROR);
        }
        return deleteResponse;
    }

    public GenericDataDTO getCustomer(String uuid) {
        GenericDataDTO customerResponse = new GenericDataDTO();
        Optional<Customer> customer = customerRepository.findByUuidAndIsDelete(uuid,false);
        if(customer.isPresent()) {
            CustomerDTO customerDTO = customerMapper.toDto(customer.get());
            customerResponse.setData(customerDTO);
            customerResponse.setResponseCode(CommonStatusCode.SUCCESS);
            customerResponse.setResponseMessege("Customer found successfully!");
        }else {
            customerResponse.setResponseCode(CommonStatusCode.SUCCESS);
            customerResponse.setResponseMessege("Customer not found!");
        }
        return customerResponse;
    }
}
