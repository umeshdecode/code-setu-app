package com.cargosetu.customer.service;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.customer.dtos.ChargesDTO;
import com.cargosetu.customer.dtos.CustomerDTO;
import com.cargosetu.customer.dtos.CustomerUpdateDto;
import com.cargosetu.customer.dtos.ExtraChargesDTO;
import com.cargosetu.customer.dtos.ShiftingDTO;
import com.cargosetu.customer.entity.Customer;
import com.cargosetu.customer.entity.ExtraCharges;
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

        String chargesError = validateShiftingCharges(customerDTO.getShiftings());
        if (chargesError != null) return new ValidationResponse(false, chargesError);

        return validationResponse;
    }

    /**
     * CustomerDTO (and its nested ShiftingDTO/ChargesDTO/ExtraChargesDTO) carry no Bean
     * Validation annotations and are never passed through {@code @Valid} — this service's
     * manual checks are the only gate on their content. Without this, a negative charge or
     * rate would be persisted as-is.
     */
    private String validateShiftingCharges(List<ShiftingDTO> shiftings) {
        if (shiftings == null) return null;

        for (int i = 0; i < shiftings.size(); i++) {
            ChargesDTO charges = shiftings.get(i).getChargesDTO();
            if (charges == null) continue;

            String label = "Shifting " + (i + 1) + ": ";

            if (isNegative(charges.getTransportCharge()))    return label + "Transport charge cannot be negative";
            if (isNegative(charges.getPackingCharge()))      return label + "Packing charge cannot be negative";
            if (isNegative(charges.getLoadingCharge()))      return label + "Loading charge cannot be negative";
            if (isNegative(charges.getUnloadingCharge()))    return label + "Unloading charge cannot be negative";
            if (isNegative(charges.getUnpackingCharge()))    return label + "Unpacking charge cannot be negative";
            if (isNegative(charges.getCarTransportCharge())) return label + "Car transport charge cannot be negative";
            if (isNegative(charges.getRiskInsureCharge()))   return label + "Risk/insurance charge cannot be negative";
            if (isNegative(charges.getStorageCharge()))      return label + "Storage charge cannot be negative";
            if (isNegative(charges.getStatisticalCharge()))  return label + "Statistical charge cannot be negative";
            if (isNegative(charges.getServiceCharge()))      return label + "Service charge cannot be negative";
            if (isNegative(charges.getGstCharge()))           return label + "GST charge cannot be negative";
            if (isNegative(charges.getServiceRate()) || isOutsidePercentRange(charges.getServiceRate()))
                return label + "Service rate must be between 0 and 100";
            if (isNegative(charges.getGstRate()) || isOutsidePercentRange(charges.getGstRate()))
                return label + "GST rate must be between 0 and 100";

            if (charges.getExtraChargesList() != null) {
                for (ExtraChargesDTO extra : charges.getExtraChargesList()) {
                    if (extra == null) continue;
                    if (isNegative(extra.getChargeValue()) || isNegative(extra.getChargeRate())) {
                        return label + "Extra charge \"" + (extra.getChargeName() != null ? extra.getChargeName() : "")
                                + "\" cannot be negative";
                    }
                }
            }
        }
        return null;
    }

    private boolean isNegative(Number value) {
        return value != null && value.doubleValue() < 0;
    }

    private boolean isOutsidePercentRange(Number value) {
        return value != null && value.doubleValue() > 100;
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
            for (int i = 0; i < customer.getShiftings().size(); i++) {
                Shifting shifting = customer.getShiftings().get(i);
                shifting.setCompanyId(customer.getCompanyId());
                shifting.setCustomer(customer);
                boolean isCompleted = shifting.isShiftCompleted();
                if (customerDTO.getShiftings() != null && i < customerDTO.getShiftings().size()) {
                    isCompleted = isCompleted || customerDTO.getShiftings().get(i).isShiftCompleted();
                }
                if (customerDTO.isShiftCompleted() && customer.getShiftings().size() == 1) {
                    isCompleted = true;
                }
                shifting.setShiftCompleted(isCompleted);

                if (shifting.getCharge() != null) {
                    if (shifting.getCharge().getExtraCharges() != null) {
                        List<ExtraCharges> uniqueExtras = new ArrayList<>();
                        for (ExtraCharges ec : shifting.getCharge().getExtraCharges()) {
                            boolean isDup = uniqueExtras.stream().anyMatch(u ->
                                (ec.getExtraChargesId() != null && ec.getExtraChargesId().equals(u.getExtraChargesId())) ||
                                (Objects.equals(u.getChargeName(), ec.getChargeName()) &&
                                 Objects.equals(u.getChargeValue(), ec.getChargeValue()))
                            );
                            if (!isDup) {
                                uniqueExtras.add(ec);
                            }
                        }
                        shifting.getCharge().setExtraCharges(uniqueExtras);
                    }
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

        String chargesError = validateShiftingCharges(customerDTO.getShiftings());
        if (chargesError != null) return new ValidationResponse(false, chargesError);

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

                    // Link each shifting back to the customer and ensure completion status is set accurately
                    for (int i = 0; i < list.size(); i++) {
                        Shifting shifting = list.get(i);
                        shifting.setCustomer(customer);
                        shifting.setCompanyId(customer.getCompanyId());
                        boolean shiftCompleted = shiftings.get(i).isShiftCompleted()
                                || (customerDTO.isShiftCompleted() && list.size() == 1);
                        shifting.setShiftCompleted(shiftCompleted);

                        // Deduplicate extra charges on update
                        if (shifting.getCharge() != null && shifting.getCharge().getExtraCharges() != null) {
                            List<ExtraCharges> uniqueExtras = new ArrayList<>();
                            for (ExtraCharges ec : shifting.getCharge().getExtraCharges()) {
                                boolean isDup = uniqueExtras.stream().anyMatch(u ->
                                    (ec.getExtraChargesId() != null && ec.getExtraChargesId().equals(u.getExtraChargesId())) ||
                                    (Objects.equals(u.getChargeName(), ec.getChargeName()) &&
                                     Objects.equals(u.getChargeValue(), ec.getChargeValue()))
                                );
                                if (!isDup) {
                                    uniqueExtras.add(ec);
                                }
                            }
                            shifting.getCharge().setExtraCharges(uniqueExtras);
                        }
                    }

                    List<Shifting> savedShiftings = shiftingRepository.saveAll(list);
                    customer.setShiftings(savedShiftings);
                } else if (customerDTO.isShiftCompleted()) {
                    if (customer.getShiftings() != null && !customer.getShiftings().isEmpty()) {
                        for (Shifting s : customer.getShiftings()) {
                            s.setShiftCompleted(true);
                        }
                        shiftingRepository.saveAll(customer.getShiftings());
                    }
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
