package com.cargosetu.customer.service;

import com.cargosetu.config.dto.LoggedInUser;
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
import com.cargosetu.utils.ApplicationUtils;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import com.cargosetu.utils.UUIDGenerator;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
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
    public GenericDataDTO createCustomer(LoggedInUser user, CustomerDTO customerDTO) {
        GenericDataDTO genericDataDTO = new GenericDataDTO();
        log.info("Creating customer with firstName={}, lastName={}, email={}, companyId={}",
                customerDTO.getFirstName(), customerDTO.getLastName(),
                customerDTO.getEmail(), customerDTO.getCompanyId());

        Customer customer = customerMapper.toEntity(customerDTO);
        String generatedUUID = UUIDGenerator.generate();
        customer.setUuid(generatedUUID);
        customer.setIsDelete(false);
        customer.setCompanyId(user.getCompanyId() != null ? user.getCompanyId() : ApplicationUtils.getCurrentCompanyId());

        // Link shiftings to customer and ensure charges cascade properly
        if (customer.getShiftings() != null) {
            log.debug("Customer has {} shifting(s) to create", customer.getShiftings().size());
            for (Shifting shifting : customer.getShiftings()) {
                shifting.setCompanyId(customer.getCompanyId());
                shifting.setCustomer(customer);
                if (shifting.getCharge() != null) {
                    log.debug("Shifting has charges with {} extra charge(s)",
                            shifting.getCharge().getExtraCharges() != null
                                    ? shifting.getCharge().getExtraCharges().size() : 0);
                }
            }
        }

        customer = customerRepository.save(customer);
        log.info("Customer saved with customerId={}, uuid={}", customer.getCustomerId(), customer.getUuid());

        if(Objects.nonNull(customer.getCustomerId())) {

            genericDataDTO.setResponseMessege("customer created successfully!");
            genericDataDTO.setResponseCode(CommonStatusCode.CREATED);
            genericDataDTO.setData(customerMapper.toDto(customer));

            // Publish generic events for email and SMS notification
            Map<String, Object> variables = Map.of(
                "customerId", customer.getCustomerId(),
                "email", customer.getEmail() != null ? customer.getEmail() : "",
                "name", customer.getFirstName() + " " + (customer.getLastName() != null ? customer.getLastName() : ""),
                "mobileNo", customer.getMobileNo() != null ? customer.getMobileNo() : ""
            );
            if (customer.getEmail() != null && !customer.getEmail().isBlank()) {
                log.debug("Publishing EMAIL notification for customer creation, email={}", customer.getEmail());
                eventPublisher.publishEvent(new GenericNotificationEvent(
                    "CUSTOMER_CREATED",
                    List.of(customer.getEmail()),
                    NotificationChannel.EMAIL,
                    variables
                ));
            }
            if (customer.getMobileNo() != null && !customer.getMobileNo().isBlank()) {
                log.debug("Publishing SMS notification for customer creation, mobileNo={}", customer.getMobileNo());
                eventPublisher.publishEvent(new GenericNotificationEvent(
                    "CUSTOMER_CREATED",
                    List.of(customer.getMobileNo()),
                    NotificationChannel.SMS,
                    variables
                ));
            }
        }else  {
            log.warn("Customer save returned null customerId");
            genericDataDTO.setResponseMessege("customer not created!");
            genericDataDTO.setResponseCode(CommonStatusCode.FAIL);
        }
        return genericDataDTO;
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

    @Transactional
    public GenericDataDTO updateCustomer(CustomerDTO customerDTO) {
        GenericDataDTO updateResponse = new GenericDataDTO();
        log.info("Updating customer with customerId={}", customerDTO.getCustomerId());

        try {
            Optional<Customer> dbCust = customerRepository.findByCustomerIdAndIsDelete(customerDTO.getCustomerId(),false);
            if(dbCust.isPresent()) {
                Customer customer = dbCust.get();
                log.debug("Found existing customer with customerId={}, uuid={}",
                        customer.getCustomerId(), customer.getUuid());

                if (customerDTO.getFirstName() != null) customer.setFirstName(customerDTO.getFirstName());
                if (customerDTO.getLastName() != null) customer.setLastName(customerDTO.getLastName());
                if (customerDTO.getEmail() != null) customer.setEmail(customerDTO.getEmail());
                if (customerDTO.getMobileNo() != null) customer.setMobileNo(customerDTO.getMobileNo());
                if (customerDTO.getAlternateMobileNo() != null) customer.setAlternateMobileNo(customerDTO.getAlternateMobileNo());
                if (customerDTO.getWorkEmail() != null) customer.setWorkEmail(customerDTO.getWorkEmail());

                List<ShiftingDTO> shiftings = customerDTO.getShiftings();
                if (shiftings != null && !shiftings.isEmpty()) {
                    log.debug("Updating {} shifting(s) for customer", shiftings.size());
                    List<Shifting> list = new ArrayList<>(shiftings.stream()
                            .map(shiftingMapper::toEntity)
                            .toList());

                    // Link each shifting back to the customer
                    for (Shifting shifting : list) {
                        shifting.setCustomer(customer);
                        shifting.setCompanyId(customer.getCompanyId());
                    }

                    shiftingRepository.saveAll(list);
                    customer.setShiftings(list);
                }

                customer = customerRepository.save(customer);
                log.info("Customer updated successfully with customerId={}", customer.getCustomerId());

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
                    log.debug("Publishing EMAIL notification for customer update, email={}", customer.getEmail());
                    eventPublisher.publishEvent(new GenericNotificationEvent(
                        "CUSTOMER_UPDATED",
                        List.of(customer.getEmail()),
                        NotificationChannel.EMAIL,
                        variables
                    ));
                }
            }else {
                log.warn("Customer not found for update, customerId={}", customerDTO.getCustomerId());
                updateResponse.setResponseCode(CommonStatusCode.NOT_FOUND);
                updateResponse.setResponseMessege("Customer not found");
            }
        } catch (Exception e) {
            log.error("Error while updating customer customerId={}: {}", customerDTO.getCustomerId(), e.getMessage(), e);
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
        log.info("Deleting customer with uuid={}", deleteDto.getUuid());
        Optional<Customer> customer = customerRepository.findByUuidAndIsDelete(deleteDto.getUuid(),false);
        if(customer.isPresent()){
            customer.get().setIsDelete(true);
            customerRepository.save(customer.get());
            log.info("Customer soft-deleted, uuid={}", deleteDto.getUuid());
            deleteResponse.setResponseCode(CommonStatusCode.SUCCESS);
            deleteResponse.setResponseMessege(CommonStatusCode.MSG_DELETED);
        }else {
            log.warn("Customer not found for delete, uuid={}", deleteDto.getUuid());
            deleteResponse.setResponseCode(CommonStatusCode.FAIL);
            deleteResponse.setResponseMessege(CommonStatusCode.MSG_INTERNAL_SERVER_ERROR);
        }
        return deleteResponse;
    }

    public GenericDataDTO getCustomer(String uuid) {
        GenericDataDTO customerResponse = new GenericDataDTO();
        log.info("Fetching customer by uuid={}", uuid);
        Optional<Customer> customer = customerRepository.findByUuidAndIsDelete(uuid,false);
        if(customer.isPresent()) {
            CustomerDTO customerDTO = customerMapper.toDto(customer.get());
            customerResponse.setData(customerDTO);
            customerResponse.setResponseCode(CommonStatusCode.SUCCESS);
            customerResponse.setResponseMessege("Customer found successfully!");
        }else {
            log.warn("Customer not found, uuid={}", uuid);
            customerResponse.setResponseCode(CommonStatusCode.SUCCESS);
            customerResponse.setResponseMessege("Customer not found!");
        }
        return customerResponse;
    }

    public GenericDataDTO getAllCustomer(LoggedInUser user,Integer pageNumber, Integer pageSize) {
        GenericDataDTO response = new GenericDataDTO();
        Page<Customer> customers = null;
        try {
            log.info("Fetching all customers, page={}, size={}, staffId={}, companyId={}",
                    pageNumber, pageSize, user.getStaffId(), user.getCompanyId());
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
