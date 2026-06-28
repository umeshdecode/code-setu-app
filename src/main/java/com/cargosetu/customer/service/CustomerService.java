package com.cargosetu.customer.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.customer.dtos.ChargesDTO;
import com.cargosetu.customer.dtos.CustomerDTO;
import com.cargosetu.customer.dtos.CustomerUpdateDto;
import com.cargosetu.customer.dtos.ShiftingDTO;
import com.cargosetu.customer.entity.Customer;
import com.cargosetu.customer.entity.Shifting;
import com.cargosetu.customer.repository.CustomerRepository;
import com.cargosetu.dto.ValidationResponse;
import com.cargosetu.mapper.CustomerMapper;
import com.cargosetu.mapper.ShiftingMapper;
import com.cargosetu.repo.ShiftingRepository;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import com.cargosetu.utils.UUIDGenerator;
import jakarta.persistence.GeneratedValue;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import com.cargosetu.notification.event.GenericNotificationEvent;
import com.cargosetu.notification.model.NotificationChannel;
import java.util.Map;

import java.util.ArrayList;
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

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ShiftingMapper shiftingMapper;

    @Autowired
    private ShiftingRepository shiftingRepository;

    public ValidationResponse validateCustomerCreateRequest(CustomerDTO customerDTO) {
        ValidationResponse validationResponse = new ValidationResponse(true, CommonStatusCode.MSG_VALIDATION_SUCCESS);
        if(Objects.isNull(customerDTO)) return  new ValidationResponse(false, "Invalid Request!!");
        if(customerDTO.getFirstName() == null) return new ValidationResponse(false, "customer name is required!!");

        return validationResponse;
    }

    @Transactional
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

            // Publish generic events for email and SMS notification
            Map<String, Object> variables = Map.of(
                "customerId", customer.getCustomerId(),
                "email", customer.getEmail() != null ? customer.getEmail() : "",
                "name", customer.getFirstName() + " " + (customer.getLastName() != null ? customer.getLastName() : ""),
                "mobileNo", customer.getMobileNo() != null ? customer.getMobileNo() : ""
            );
            if (customer.getEmail() != null && !customer.getEmail().isBlank()) {
                eventPublisher.publishEvent(new GenericNotificationEvent(
                    "CUSTOMER_CREATED",
                    List.of(customer.getEmail()),
                    NotificationChannel.EMAIL,
                    variables
                ));
            }
            if (customer.getMobileNo() != null && !customer.getMobileNo().isBlank()) {
                eventPublisher.publishEvent(new GenericNotificationEvent(
                    "CUSTOMER_CREATED",
                    List.of(customer.getMobileNo()),
                    NotificationChannel.SMS,
                    variables
                ));
            }
        }else  {
            genericDataDTO.setResponseMessege("customer not created!");
            genericDataDTO.setResponseCode(CommonStatusCode.FAIL);
        }
        return genericDataDTO;
    }

    private void linkCustomerToShifting(List<Shifting> shiftings, Customer customer) {
        for(Shifting shifting : shiftings) {
            shifting.setCompanyId(customer.getCompanyId());
            shifting.setCustomer(customer);
        }
    }


    public ValidationResponse validateCustomerUpdateRequest(CustomerDTO customerDTO) {
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

    public GenericDataDTO updateCustomer(CustomerDTO customerDTO) {
        GenericDataDTO updateResponse = new GenericDataDTO();
        try {
            Optional<Customer> dbCust = customerRepository.findByCustomerIdAndIsDelete(customerDTO.getCustomerId(),false);
            if(dbCust.isPresent()) {
                Customer customer = dbCust.get();
                if (customerDTO.getFirstName() != null) customer.setFirstName(customerDTO.getFirstName());
                if (customerDTO.getLastName() != null) customer.setLastName(customerDTO.getLastName());
                if (customerDTO.getEmail() != null) customer.setEmail(customerDTO.getEmail());
                if (customerDTO.getMobileNo() != null) customer.setMobileNo(customerDTO.getMobileNo());
                if (customerDTO.getAlternateMobileNo() != null) customer.setAlternateMobileNo(customerDTO.getAlternateMobileNo());
                if (customerDTO.getWorkEmail() != null) customer.setWorkEmail(customerDTO.getWorkEmail());

                List<ShiftingDTO> shiftings = customerDTO.getShiftings();
                if (shiftings != null && !shiftings.isEmpty()) {
                    List<Shifting> list = shiftings.stream()
                            .map(shifting -> shiftingMapper.toEntity(shifting))
                            .toList();
                    shiftingRepository.saveAll(list);
                    customer.setShiftings(list);
                }

                customer = customerRepository.save(customer);
                updateResponse.setData(customerMapper.toDto(customer));
                updateResponse.setResponseCode(CommonStatusCode.SUCCESS);
                updateResponse.setResponseMessege("customer updated successfully!");

                Map<String, Object> variables = Map.of(
                    "customerId", customer.getCustomerId(),
                    "email", customer.getEmail() != null ? customer.getEmail() : "",
                    "name", customer.getFirstName() + " " + (customer.getLastName() != null ? customer.getLastName() : ""),
                    "mobileNo", customer.getMobileNo() != null ? customer.getMobileNo() : ""
                );
                if (customer.getEmail() != null && !customer.getEmail().isBlank()) {
                    eventPublisher.publishEvent(new GenericNotificationEvent(
                        "CUSTOMER_UPDATED",
                        List.of(customer.getEmail()),
                        NotificationChannel.EMAIL,
                        variables
                    ));
                }
            }else {
                updateResponse.setResponseCode(CommonStatusCode.NOT_FOUND);
                updateResponse.setResponseMessege("Customer not found");
            }
        } catch (Exception e) {
            log.error("error while updating customer: {}", e.getMessage(), e);
            updateResponse.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            updateResponse.setResponseMessege("An error occurred while updating customer");
            updateResponse.setErrors(new Object[]{e.getMessage()});
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

    public GenericDataDTO getAllCustomer(LoggedInUser user,Integer pageNumber, Integer pageSize) {
        GenericDataDTO response = new GenericDataDTO();
        Page<Customer> customers = null;
        try {
            log.info("Fetching all customers from database");
            Pageable pageable = PageRequest.of(pageNumber, pageSize);

            if(user.getStaffId() == 1){
                 customers = customerRepository.findByIsDeleteOrderByCustomerIdDesc
                        (false, pageable);
            }else{
                if(user.getCompanyId() == null){
                    response.setResponseCode(CommonStatusCode.NOT_FOUND);
                    response.setResponseMessege("Please Create your Company");
                    return response;
                }
                customers = customerRepository.findByCompanyIdAndIsDeleteOrderByCustomerIdDesc
                        (user.getCompanyId(),false, pageable);
            }


            if (customers.isEmpty()) {
                log.warn("No customers found in database");
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege(CommonStatusCode.MSG_NOT_FOUND);
                return response;
            }

            Page<CustomerDTO> customerDTOs = customers.map(customerMapper::toDto);

            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege(CommonStatusCode.MSG_SUCCESS);
            // Returning the Page object preserves pagination metadata
            response.setData(customerDTOs);

        } catch (DataAccessException dae) {
            // Database-related issues
            log.error("Database error while fetching customers", dae);

            response.setResponseCode(CommonStatusCode.FAIL);
            response.setResponseMessege("Database error occurred while fetching customers");

        } catch (Exception e) {
            // Anything unexpected
            log.error("Unexpected error while fetching customers", e);

            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("Internal server error");
        }

        return response;
    }
}
