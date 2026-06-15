package com.cargosetu.service;

import com.cargosetu.dto.TripStatusHistoryDTO;
import com.cargosetu.mapper.TripStatusHistoryMapper;
import com.cargosetu.model.TripStatusHistory;
import com.cargosetu.repo.TripRepository;
import com.cargosetu.repo.TripStatusHistoryRepository;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class TripStatusHistoryService {

    private final TripStatusHistoryRepository repository;
    private final TripStatusHistoryMapper mapper;
    private final TripRepository tripRepository;

    public TripStatusHistoryService(TripStatusHistoryRepository repository,
                                    TripStatusHistoryMapper mapper,
                                    TripRepository tripRepository) {
        this.repository = repository;
        this.mapper = mapper;
        this.tripRepository = tripRepository;
    }

    @Transactional
    public GenericDataDTO create(TripStatusHistoryDTO dto) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            TripStatusHistory entity = mapper.toEntity(dto);

            if (dto.getTripId() != null) {
                tripRepository.findById(dto.getTripId()).ifPresent(entity::setTrip);
            }

            entity.setChangedAt(LocalDateTime.now());

            TripStatusHistory saved = repository.save(entity);
            response.setResponseCode(CommonStatusCode.CREATED);
            response.setResponseMessege("Trip status history created successfully!");
            response.setData(mapper.toDto(saved));
        } catch (Exception e) {
            log.error("Error creating trip status history: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred.");
            response.setErrors(new Object[]{e.getMessage()});
        }
        return response;
    }

    public GenericDataDTO getById(Long id) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<TripStatusHistory> entity = repository.findById(id);
            if (entity.isPresent()) {
                response.setResponseCode(CommonStatusCode.SUCCESS);
                response.setResponseMessege("Trip status history found!");
                response.setData(mapper.toDto(entity.get()));
            } else {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Trip status history not found with ID: " + id);
            }
        } catch (Exception e) {
            log.error("Error fetching trip status history {}: {}", id, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred.");
        }
        return response;
    }

    public GenericDataDTO getAll() {
        GenericDataDTO response = new GenericDataDTO();
        try {
            List<TripStatusHistoryDTO> list = repository.findAll().stream()
                    .map(mapper::toDto)
                    .toList();
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Trip status histories fetched successfully!");
            response.setData(list);
        } catch (Exception e) {
            log.error("Error fetching all trip status histories: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred.");
        }
        return response;
    }

    public GenericDataDTO getByTripId(Long tripId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            List<TripStatusHistoryDTO> list = repository.findByTripTripIdOrderByChangedAtDesc(tripId)
                    .stream()
                    .map(mapper::toDto)
                    .toList();
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Trip status histories fetched for trip!");
            response.setData(list);
        } catch (Exception e) {
            log.error("Error fetching status history for trip {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred.");
        }
        return response;
    }

    @Transactional
    public GenericDataDTO update(Long id, TripStatusHistoryDTO dto) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<TripStatusHistory> existing = repository.findById(id);
            if (existing.isEmpty()) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Trip status history not found with ID: " + id);
                return response;
            }

            TripStatusHistory entity = existing.get();
            if (dto.getOldStatus() != null) entity.setOldStatus(dto.getOldStatus());
            if (dto.getNewStatus() != null) entity.setNewStatus(dto.getNewStatus());
            if (dto.getChangedBy() != null) entity.setChangedBy(dto.getChangedBy());
            if (dto.getTripId() != null) {
                tripRepository.findById(dto.getTripId()).ifPresent(entity::setTrip);
            }

            TripStatusHistory saved = repository.save(entity);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Trip status history updated successfully!");
            response.setData(mapper.toDto(saved));
        } catch (Exception e) {
            log.error("Error updating trip status history {}: {}", id, e.getMessage(), e);
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
                response.setResponseMessege("Trip status history deleted successfully!");
            } else {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Trip status history not found with ID: " + id);
            }
        } catch (Exception e) {
            log.error("Error deleting trip status history {}: {}", id, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred.");
        }
        return response;
    }
}
