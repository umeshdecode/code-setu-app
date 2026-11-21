package com.cargosetu.service;

import com.cargosetu.dto.TripExpenseDTO;
import com.cargosetu.mapper.TripExpenseMapper;
import com.cargosetu.model.TripExpense;
import com.cargosetu.repo.TripExpenseRepository;
import com.cargosetu.repo.TripRepository;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class TripExpenseService {

    private final TripExpenseRepository repository;
    private final TripExpenseMapper mapper;
    private final TripRepository tripRepository;

    public TripExpenseService(TripExpenseRepository repository,
                              TripExpenseMapper mapper,
                              TripRepository tripRepository) {
        this.repository = repository;
        this.mapper = mapper;
        this.tripRepository = tripRepository;
    }

    @Transactional
    public GenericDataDTO create(TripExpenseDTO dto) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            TripExpense entity = mapper.toEntity(dto);
            if (dto.getTripId() != null) {
                tripRepository.findById(dto.getTripId()).ifPresent(entity::setTrip);
            }
            TripExpense saved = repository.save(entity);
            response.setResponseCode(CommonStatusCode.CREATED);
            response.setResponseMessege("Trip expense created successfully!");
            response.setData(mapper.toDto(saved));
        } catch (Exception e) {
            log.error("Error creating trip expense: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred.");
            response.setErrors(new Object[]{e.getMessage()});
        }
        return response;
    }

    public GenericDataDTO getById(Long id) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<TripExpense> entity = repository.findById(id);
            if (entity.isPresent()) {
                response.setResponseCode(CommonStatusCode.SUCCESS);
                response.setResponseMessege("Trip expense found!");
                response.setData(mapper.toDto(entity.get()));
            } else {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Trip expense not found with ID: " + id);
            }
        } catch (Exception e) {
            log.error("Error fetching trip expense {}: {}", id, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred.");
        }
        return response;
    }

    public GenericDataDTO getAll() {
        GenericDataDTO response = new GenericDataDTO();
        try {
            List<TripExpenseDTO> list = repository.findAll().stream()
                    .map(mapper::toDto)
                    .toList();
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Trip expenses fetched successfully!");
            response.setData(list);
        } catch (Exception e) {
            log.error("Error fetching all trip expenses: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred.");
        }
        return response;
    }

    public GenericDataDTO getByTripId(Long tripId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            List<TripExpenseDTO> list = repository.findByTripTripId(tripId).stream()
                    .map(mapper::toDto)
                    .toList();
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Trip expenses fetched for trip!");
            response.setData(list);
        } catch (Exception e) {
            log.error("Error fetching expenses for trip {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred.");
        }
        return response;
    }

    @Transactional
    public GenericDataDTO update(Long id, TripExpenseDTO dto) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<TripExpense> existing = repository.findById(id);
            if (existing.isEmpty()) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Trip expense not found with ID: " + id);
                return response;
            }

            TripExpense entity = existing.get();
            if (dto.getExpenseType() != null) entity.setExpenseType(dto.getExpenseType());
            if (dto.getAmount() != null) entity.setAmount(dto.getAmount());
            if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
            if (dto.getBillNumber() != null) entity.setBillNumber(dto.getBillNumber());
            if (dto.getReceiptUrl() != null) entity.setReceiptUrl(dto.getReceiptUrl());
            if (dto.getExpenseDate() != null) entity.setExpenseDate(dto.getExpenseDate());
            if (dto.getApproved() != null) entity.setApproved(dto.getApproved());
            if (dto.getTripId() != null) {
                tripRepository.findById(dto.getTripId()).ifPresent(entity::setTrip);
            }

            TripExpense saved = repository.save(entity);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Trip expense updated successfully!");
            response.setData(mapper.toDto(saved));
        } catch (Exception e) {
            log.error("Error updating trip expense {}: {}", id, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred.");
            response.setErrors(new Object[]{e.getMessage()});
        }
        return response;
    }

    @Transactional
    public GenericDataDTO delete(Long id) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            if (repository.existsById(id)) {
                repository.deleteById(id);
                response.setResponseCode(CommonStatusCode.SUCCESS);
                response.setResponseMessege("Trip expense deleted successfully!");
            } else {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Trip expense not found with ID: " + id);
            }
        } catch (Exception e) {
            log.error("Error deleting trip expense {}: {}", id, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred.");
        }
        return response;
    }
}
