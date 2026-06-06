package com.cargosetu.customer.service;

import com.cargosetu.customer.dtos.ShiftingDTO;
import com.cargosetu.customer.entity.Customer;
import com.cargosetu.customer.entity.Shifting;
import com.cargosetu.customer.repository.CustomerRepository;
import com.cargosetu.mapper.ShiftingMapper;
import com.cargosetu.repo.ShiftingRepository;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class ShiftingService {

    private final ShiftingRepository shiftingRepository;
    private final ShiftingMapper shiftingMapper;
    private final CustomerRepository customerRepository; // To link shifting to a customer

    public ShiftingService(ShiftingRepository shiftingRepository,
                           ShiftingMapper shiftingMapper,
                           CustomerRepository customerRepository) {
        this.shiftingRepository = shiftingRepository;
        this.shiftingMapper = shiftingMapper;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public GenericDataDTO createShifting(ShiftingDTO shiftingDTO) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (shiftingDTO.getCustomerId() == null) {
                response.setResponseCode(CommonStatusCode.VALIDATION_ERROR);
                response.setResponseMessege("Customer ID is required for shifting.");
                return response;
            }

            Optional<Customer> customerOptional = customerRepository.findById(shiftingDTO.getCustomerId());
            if (customerOptional.isEmpty()) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Customer not found with ID: " + shiftingDTO.getCustomerId());
                return response;
            }

            Shifting shifting = shiftingMapper.toEntity(shiftingDTO);
            shifting.setCustomer(customerOptional.get()); // Link the shifting to the customer

            Shifting savedShifting = shiftingRepository.save(shifting);
            response.setResponseCode(CommonStatusCode.CREATED);
            response.setResponseMessege("Shifting created successfully!");
            response.setData(shiftingMapper.toDto(savedShifting));

        } catch (Exception e) {
            log.error("Error creating shifting: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while creating the shifting.");
        }
        return response;
    }

    public GenericDataDTO getShifting(Long shiftId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<Shifting> shiftingOptional = shiftingRepository.findById(shiftId);
            if (shiftingOptional.isPresent()) {
                response.setResponseCode(CommonStatusCode.SUCCESS);
                response.setResponseMessege("Shifting found successfully!");
                response.setData(shiftingMapper.toDto(shiftingOptional.get()));
            } else {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Shifting not found with ID: " + shiftId);
            }
        } catch (Exception e) {
            log.error("Error getting shifting with ID {}: {}", shiftId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while retrieving the shifting.");
        }
        return response;
    }

    @Transactional
    public GenericDataDTO updateShifting(ShiftingDTO shiftingDTO) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (shiftingDTO.getShiftingId() == null) {
                response.setResponseCode(CommonStatusCode.VALIDATION_ERROR);
                response.setResponseMessege("Shifting ID is required for update.");
                return response;
            }

            Optional<Shifting> existingShiftingOptional = shiftingRepository.findById(shiftingDTO.getShiftingId());
            if (existingShiftingOptional.isEmpty()) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Shifting not found with ID: " + shiftingDTO.getShiftingId());
                return response;
            }

            Shifting existingShifting = existingShiftingOptional.get();

            // Update customer if changed
            if (shiftingDTO.getCustomerId() != null && !Objects.equals(existingShifting.getCustomer().getCustomerId(), shiftingDTO.getCustomerId())) {
                Optional<Customer> customerOptional = customerRepository.findById(shiftingDTO.getCustomerId());
                if (customerOptional.isEmpty()) {
                    response.setResponseCode(CommonStatusCode.NOT_FOUND);
                    response.setResponseMessege("Customer not found with ID: " + shiftingDTO.getCustomerId());
                    return response;
                }
                existingShifting.setCustomer(customerOptional.get());
            }

            // Use mapper to update properties. Assuming BaseUpdateMapper has updateFromDto
            // If not, you'd manually copy properties or re-map and set ID.
            // For now, let's manually copy for clarity if updateFromDto is not explicitly defined here.
            // A better approach would be to implement updateFromDto in BaseUpdateMapper and use it.
            Shifting updatedShifting = shiftingMapper.toEntity(shiftingDTO);
            updatedShifting.setShiftingId(existingShifting.getShiftingId()); // Ensure ID is preserved for update

            // Copy properties from updatedShifting to existingShifting
            existingShifting.setShiftCompleted(updatedShifting.isShiftCompleted());
            existingShifting.setShiftFromCity(updatedShifting.getShiftFromCity());
            existingShifting.setShiftToCity(updatedShifting.getShiftToCity());
            existingShifting.setToAddress(updatedShifting.getToAddress());
            existingShifting.setFromAddress(updatedShifting.getFromAddress());
            existingShifting.setShiftingType(updatedShifting.getShiftingType());
            existingShifting.setShiftingStartDate(updatedShifting.getShiftingStartDate());
            existingShifting.setShiftReachedDate(updatedShifting.getShiftReachedDate());
            existingShifting.setShiftCompletedBy(updatedShifting.getShiftCompletedBy());
            existingShifting.setShiftCompletedById(updatedShifting.getShiftCompletedById());
            // Assuming charges are handled by ChargesMapper within ShiftingMapper
            existingShifting.setCharge(updatedShifting.getCharge());


            Shifting savedShifting = shiftingRepository.save(existingShifting);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Shifting updated successfully!");
            response.setData(shiftingMapper.toDto(savedShifting));

        } catch (Exception e) {
            log.error("Error updating shifting with ID {}: {}", shiftingDTO.getShiftingId(), e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred while updating the shifting.");
        }
        return response;
    }
}