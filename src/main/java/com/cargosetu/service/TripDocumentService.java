package com.cargosetu.service;

import com.cargosetu.dto.TripDocumentDTO;
import com.cargosetu.mapper.TripDocumentMapper;
import com.cargosetu.model.TripDocument;
import com.cargosetu.repo.ClientServiceRepo;
import com.cargosetu.repo.TripDocumentRepository;
import com.cargosetu.repo.TripRepository;
import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import com.cargosetu.utils.utillity.FileUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class TripDocumentService {

    private static final String UPLOAD_DIR = "./uploads/trip-documents/";

    private final TripDocumentRepository repository;
    private final TripDocumentMapper mapper;
    private final TripRepository tripRepository;
    private final FileUtility fileUtility;
    private final ClientServiceRepo clientServiceRepo;

    public TripDocumentService(TripDocumentRepository repository,
                               TripDocumentMapper mapper,
                               TripRepository tripRepository,
                               FileUtility fileUtility, ClientServiceRepo clientServiceRepo) {
        this.repository = repository;
        this.mapper = mapper;
        this.tripRepository = tripRepository;
        this.fileUtility = fileUtility;
        this.clientServiceRepo = clientServiceRepo;
    }

    @Transactional
    public GenericDataDTO create(MultipartFile file, Long tripId, String documentType) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            TripDocument entity = new TripDocument();

            if (tripId != null) {
                tripRepository.findById(tripId).ifPresent(entity::setTrip);
            }
            entity.setDocumentType(documentType);
            String savedFileName = null;
            if (file != null && !file.isEmpty()) {
                List<String> filePath = FileUtility.saveMultipartFile(new MultipartFile[]{file}, clientServiceRepo.findByName("TRIP_DOCS").orElseThrow().getName());
                if(Objects.nonNull(filePath) && !filePath.isEmpty()){
                   savedFileName  = filePath.get(0);
                }
                entity.setFileName(file.getOriginalFilename());
                entity.setDocumentUrl(savedFileName);
            }

            TripDocument saved = repository.save(entity);
            response.setResponseCode(CommonStatusCode.CREATED);
            response.setResponseMessege("Trip document uploaded successfully!");
            response.setData(mapper.toDto(saved));
        } catch (Exception e) {
            log.error("Error uploading trip document: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred.");
            response.setErrors(new Object[]{e.getMessage()});
        }
        return response;
    }

    public GenericDataDTO getById(Long id) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<TripDocument> entity = repository.findById(id);
            if (entity.isPresent()) {
                response.setResponseCode(CommonStatusCode.SUCCESS);
                response.setResponseMessege("Trip document found!");
                response.setData(mapper.toDto(entity.get()));
            } else {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Trip document not found with ID: " + id);
            }
        } catch (Exception e) {
            log.error("Error fetching trip document {}: {}", id, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred.");
        }
        return response;
    }

    public GenericDataDTO getAll() {
        GenericDataDTO response = new GenericDataDTO();
        try {
            List<TripDocumentDTO> list = repository.findAll().stream()
                    .map(mapper::toDto)
                    .toList();
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Trip documents fetched successfully!");
            response.setData(list);
        } catch (Exception e) {
            log.error("Error fetching all trip documents: {}", e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred.");
        }
        return response;
    }

    public GenericDataDTO getByTripId(Long tripId) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            List<TripDocumentDTO> list = repository.findByTripTripId(tripId).stream()
                    .map(mapper::toDto)
                    .toList();
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Trip documents fetched for trip!");
            response.setData(list);
        } catch (Exception e) {
            log.error("Error fetching documents for trip {}: {}", tripId, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred.");
        }
        return response;
    }

    @Transactional
    public GenericDataDTO update(Long id, MultipartFile file, Long tripId, String documentType) {
        GenericDataDTO response = new GenericDataDTO();
        try {
            Optional<TripDocument> existing = repository.findById(id);
            if (existing.isEmpty()) {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Trip document not found with ID: " + id);
                return response;
            }

            TripDocument entity = existing.get();
            if (documentType != null) entity.setDocumentType(documentType);
            if (tripId != null) {
                tripRepository.findById(tripId).ifPresent(entity::setTrip);
            }

            if (file != null && !file.isEmpty()) {
                if (entity.getDocumentUrl() != null) {
                    fileUtility.removeFileAtServer(entity.getDocumentUrl(), UPLOAD_DIR);
                }
                String savedFileName = fileUtility.saveFile(file, UPLOAD_DIR);
                entity.setFileName(file.getOriginalFilename());
                entity.setDocumentUrl(savedFileName);
            }

            TripDocument saved = repository.save(entity);
            response.setResponseCode(CommonStatusCode.SUCCESS);
            response.setResponseMessege("Trip document updated successfully!");
            response.setData(mapper.toDto(saved));
        } catch (Exception e) {
            log.error("Error updating trip document {}: {}", id, e.getMessage(), e);
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
            Optional<TripDocument> entity = repository.findById(id);
            if (entity.isPresent()) {
                if (entity.get().getDocumentUrl() != null) {
                    fileUtility.removeFileAtServer(entity.get().getDocumentUrl(), UPLOAD_DIR);
                }
                repository.deleteById(id);
                response.setResponseCode(CommonStatusCode.SUCCESS);
                response.setResponseMessege("Trip document deleted successfully!");
            } else {
                response.setResponseCode(CommonStatusCode.NOT_FOUND);
                response.setResponseMessege("Trip document not found with ID: " + id);
            }
        } catch (Exception e) {
            log.error("Error deleting trip document {}: {}", id, e.getMessage(), e);
            response.setResponseCode(CommonStatusCode.INTERNAL_SERVER_ERROR);
            response.setResponseMessege("An unexpected error occurred.");
        }
        return response;
    }
}
