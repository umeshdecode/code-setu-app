package com.cargosetu.service;

import com.cargosetu.client.cardekho.CardekhoClient;
import com.cargosetu.client.cardekho.CardekhoModelDto;
import com.cargosetu.config.CacheConfig;
import com.cargosetu.dto.VehicleBrandDto;
import com.cargosetu.dto.VehicleCatalogueResponseDto;
import com.cargosetu.dto.VehicleModelDto;
import com.cargosetu.entity.VehicleBrand;
import com.cargosetu.entity.VehicleModel;
import com.cargosetu.entity.VehicleModelImage;
import com.cargosetu.entity.VehicleSpecification;
import com.cargosetu.mapper.VehicleCatalogueMapper;
import com.cargosetu.repository.VehicleBrandRepository;
import com.cargosetu.repository.VehicleModelImageRepository;
import com.cargosetu.repository.VehicleModelRepository;
import com.cargosetu.repository.VehicleSpecificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleCatalogueService {

    private final VehicleBrandRepository brandRepository;
    private final VehicleModelRepository modelRepository;
    private final VehicleSpecificationRepository specRepository;
    private final VehicleModelImageRepository imageRepository;
    private final VehicleCatalogueMapper mapper;
    private final CardekhoClient cardekhoClient;

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void autoSyncCatalogueOnStartup() {
        log.info("Starting background auto-sync of vehicle catalogue models from CarDekho...");
        List<String> brands = List.of("eicher", "tata", "ashok-leyland", "mahindra", "bharatbenz");
        for (String brandSlug : brands) {
            try {
                syncCardekhoModelsForBrand(brandSlug);
            } catch (Exception e) {
                log.warn("Failed background CarDekho auto-sync for brand {}: {}", brandSlug, e.getMessage());
            }
        }
        log.info("Completed background auto-sync of vehicle catalogue models.");
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_VEHICLE_BRANDS, key = "'all'")
    public List<VehicleBrandDto> getBrands() {
        log.info("Fetching active vehicle brands from database");
        return brandRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(mapper::toBrandDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @Cacheable(value = CacheConfig.CACHE_VEHICLE_MODELS_BY_BRAND, key = "#brandSlug != null ? #brandSlug.toLowerCase() : 'all'")
    public List<VehicleModelDto> getModelsByBrand(String brandSlug) {
        if (brandSlug == null || brandSlug.trim().isEmpty()) {
            log.info("Fetching all active vehicle models from database");
            return modelRepository.findByActiveTrueOrderByNameAsc()
                    .stream()
                    .map(mapper::toModelDto)
                    .collect(Collectors.toList());
        }

        String normalizedSlug = brandSlug.toLowerCase().trim();
        log.info("Fetching vehicle models for brand '{}' from database", normalizedSlug);
        List<VehicleModel> existing = modelRepository.findByBrandSlugAndActiveTrueOrderByNameAsc(normalizedSlug);

        // If local models count is less than 10, trigger on-demand sync from CarDekho
        if (existing.size() < 10) {
            log.info("Local model count for brand '{}' is {}, syncing from CarDekho API...", normalizedSlug, existing.size());
            syncCardekhoModelsForBrand(normalizedSlug);
            existing = modelRepository.findByBrandSlugAndActiveTrueOrderByNameAsc(normalizedSlug);
        }

        return existing.stream()
                .map(mapper::toModelDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void syncCardekhoModelsForBrand(String brandSlug) {
        if (brandSlug == null || brandSlug.trim().isEmpty()) return;
        String slug = brandSlug.toLowerCase().trim();

        VehicleBrand brand = brandRepository.findBySlug(slug)
                .orElseGet(() -> brandRepository.save(VehicleBrand.builder()
                        .slug(slug)
                        .name(capitalize(slug))
                        .active(true)
                        .source("cardekho")
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()));

        List<CardekhoModelDto> externalModels = cardekhoClient.getModelsByBrand(slug);
        if (externalModels.isEmpty()) {
            log.warn("No models returned from CarDekho for brand {}", slug);
            return;
        }

        log.info("Syncing {} models from CarDekho for brand {}", externalModels.size(), slug);
        for (CardekhoModelDto dto : externalModels) {
            if (dto.getSlug() == null || dto.getName() == null) continue;
            String modelSlug = dto.getSlug().toLowerCase().trim();

            Optional<VehicleModel> existingOpt = modelRepository.findByBrandSlugAndSlug(slug, modelSlug);
            VehicleModel model;
            if (existingOpt.isPresent()) {
                model = existingOpt.get();
                model.setName(dto.getName());
                model.setSource("cardekho");
                model.setUpdatedAt(LocalDateTime.now());
                modelRepository.save(model);
            } else {
                model = VehicleModel.builder()
                        .brand(brand)
                        .name(dto.getName())
                        .slug(modelSlug)
                        .source("cardekho")
                        .active(true)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                model = modelRepository.save(model);

                // Create basic specification shell
                VehicleSpecification spec = VehicleSpecification.builder()
                        .vehicleModel(model)
                        .fuelType("DIESEL")
                        .lastSyncedAt(LocalDateTime.now())
                        .build();
                specRepository.save(spec);
            }
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_VEHICLE_MODEL_DETAILS, key = "#id")
    public VehicleCatalogueResponseDto getModelDetails(Long id) {
        log.info("Fetching vehicle model details for id '{}' from database", id);
        VehicleModel model = modelRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle model not found with id: " + id));

        VehicleSpecification spec = specRepository.findByVehicleModel(model).orElse(null);
        List<VehicleModelImage> images = imageRepository.findByVehicleModelOrderByDisplayOrderAsc(model);

        return mapper.toCatalogueResponseDto(model, spec, images);
    }

    @Transactional(readOnly = true)
    public List<VehicleModelDto> searchModels(String keyword) {
        log.info("Searching vehicle models with keyword: '{}'", keyword);
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return modelRepository.findByNameContainingIgnoreCaseAndActiveTrue(keyword.trim())
                .stream()
                .map(mapper::toModelDto)
                .collect(Collectors.toList());
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        String[] parts = str.split("[-_\\s]+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
