package com.cargosetu.service;

import com.cargosetu.client.trucksfloor.TrucksfloorClient;
import com.cargosetu.client.trucksfloor.TrucksfloorProduct;
import com.cargosetu.client.trucksfloor.TrucksfloorResponse;
import com.cargosetu.config.CacheConfig;
import com.cargosetu.config.TrucksfloorProperties;
import com.cargosetu.dto.VehicleCatalogueSyncResultDto;
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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleCatalogueSyncService {

    private final TrucksfloorClient trucksfloorClient;
    private final VehicleBrandRepository brandRepository;
    private final VehicleModelRepository modelRepository;
    private final VehicleSpecificationRepository specRepository;
    private final VehicleModelImageRepository imageRepository;
    private final VehicleCatalogueMapper mapper;
    private final CacheManager cacheManager;
    private final TrucksfloorProperties properties;

    /**
     * Synchronizes all configured brands from external provider.
     */
    public List<VehicleCatalogueSyncResultDto> syncAllConfiguredBrands() {
        log.info("Starting synchronization for all configured brands");
        List<String> brands = properties.getSyncBrands();
        if (brands == null || brands.isEmpty()) {
            brands = List.of("tata", "eicher", "ashok-leyland", "mahindra", "bharatbenz");
        }

        List<VehicleCatalogueSyncResultDto> results = new ArrayList<>();
        for (String brandSlug : brands) {
            try {
                VehicleCatalogueSyncResultDto result = syncBrand(brandSlug.trim());
                results.add(result);
            } catch (Exception e) {
                log.error("Failed to sync brand '{}': {}", brandSlug, e.getMessage(), e);
                results.add(VehicleCatalogueSyncResultDto.builder()
                        .brand(brandSlug)
                        .pagesProcessed(0)
                        .productsProcessed(0)
                        .inserted(0)
                        .updated(0)
                        .failed(1)
                        .startedAt(LocalDateTime.now())
                        .completedAt(LocalDateTime.now())
                        .build());
            }
        }

        // Evict all catalogue caches after full sync
        evictAllCaches();
        log.info("Completed full catalogue synchronization across {} brands", results.size());
        return results;
    }

    /**
     * Synchronizes a single brand with dynamic pagination and idempotent upsert.
     */
    public VehicleCatalogueSyncResultDto syncBrand(String brandSlug) {
        String normalizedSlug = brandSlug.toLowerCase().trim();
        LocalDateTime startedAt = LocalDateTime.now();
        log.info("Starting vehicle catalogue synchronization for brand: {}", normalizedSlug);

        VehicleBrand brand = findOrCreateBrand(normalizedSlug);

        int page = 1;
        int pagesProcessed = 0;
        int productsProcessed = 0;
        int inserted = 0;
        int updated = 0;
        int failed = 0;
        Set<Long> affectedModelIds = new HashSet<>();

        while (true) {
            TrucksfloorResponse response = trucksfloorClient.getTrucks(normalizedSlug, page);
            pagesProcessed++;

            if (response == null || response.getData() == null || response.getData().isEmpty()) {
                log.info("No data returned on page {} for brand {}", page, normalizedSlug);
                break;
            }

            List<TrucksfloorProduct> products = response.getData();
            productsProcessed += products.size();

            for (TrucksfloorProduct product : products) {
                try {
                    UpsertResult upsertResult = upsertProduct(product, brand);
                    if (upsertResult.isInserted()) {
                        inserted++;
                    } else {
                        updated++;
                    }
                    if (upsertResult.getModelId() != null) {
                        affectedModelIds.add(upsertResult.getModelId());
                    }
                } catch (Exception e) {
                    failed++;
                    log.error("Failed to upsert product '{}' for brand '{}': {}",
                            product.getSlug(), normalizedSlug, e.getMessage(), e);
                }
            }

            Integer lastPage = response.getLastPage();
            if (lastPage == null || page >= lastPage) {
                log.info("Reached last page ({}) for brand {}", lastPage != null ? lastPage : page, normalizedSlug);
                break;
            }
            page++;
        }

        LocalDateTime completedAt = LocalDateTime.now();
        log.info("Completed sync for brand '{}': pages={}, products={}, inserted={}, updated={}, failed={}",
                normalizedSlug, pagesProcessed, productsProcessed, inserted, updated, failed);

        // Evict brand's models and affected model details
        evictBrandCache(normalizedSlug, affectedModelIds);

        return VehicleCatalogueSyncResultDto.builder()
                .brand(normalizedSlug)
                .pagesProcessed(pagesProcessed)
                .productsProcessed(productsProcessed)
                .inserted(inserted)
                .updated(updated)
                .failed(failed)
                .startedAt(startedAt)
                .completedAt(completedAt)
                .build();
    }

    @Transactional
    public UpsertResult upsertProduct(TrucksfloorProduct product, VehicleBrand brand) {
        String slug = product.getSlug();
        String sourceProductId = product.resolveProductId();

        // Check for existing record by (brand, slug) or (source, sourceProductId)
        Optional<VehicleModel> existingOpt = modelRepository.findByBrandAndSlug(brand, slug);
        if (existingOpt.isEmpty() && sourceProductId != null) {
            existingOpt = modelRepository.findBySourceAndSourceProductId("trucksfloor", sourceProductId);
        }

        boolean isInserted = false;
        VehicleModel model;

        if (existingOpt.isPresent()) {
            model = existingOpt.get();
            mapper.updateModelFromProduct(model, product);
            model.setUpdatedAt(LocalDateTime.now());
            model = modelRepository.save(model);
        } else {
            model = mapper.toModelEntity(product, brand);
            model.setCreatedAt(LocalDateTime.now());
            model.setUpdatedAt(LocalDateTime.now());
            model = modelRepository.save(model);
            isInserted = true;
        }

        // Upsert specification
        Optional<VehicleSpecification> specOpt = specRepository.findByVehicleModel(model);
        VehicleSpecification spec;
        if (specOpt.isPresent()) {
            spec = specOpt.get();
            mapper.updateSpecificationFromProduct(spec, product);
        } else {
            spec = mapper.toSpecificationEntity(product, model);
        }
        specRepository.save(spec);

        // Upsert images
        List<String> imageUrls = product.extractImageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            imageRepository.deleteByVehicleModel(model);
            List<VehicleModelImage> imagesToSave = new ArrayList<>();
            for (int i = 0; i < imageUrls.size(); i++) {
                imagesToSave.add(VehicleModelImage.builder()
                        .vehicleModel(model)
                        .imageUrl(imageUrls.get(i))
                        .displayOrder(i + 1)
                        .createdAt(LocalDateTime.now())
                        .build());
            }
            imageRepository.saveAll(imagesToSave);
        }

        return new UpsertResult(isInserted, model.getId());
    }

    private VehicleBrand findOrCreateBrand(String slug) {
        return brandRepository.findBySlug(slug)
                .orElseGet(() -> {
                    VehicleBrand newBrand = VehicleBrand.builder()
                            .slug(slug)
                            .name(capitalize(slug))
                            .active(true)
                            .source("trucksfloor")
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return brandRepository.save(newBrand);
                });
    }

    private void evictBrandCache(String brandSlug, Set<Long> affectedModelIds) {
        try {
            Cache brandModelsCache = cacheManager.getCache(CacheConfig.CACHE_VEHICLE_MODELS_BY_BRAND);
            if (brandModelsCache != null) {
                brandModelsCache.evict(brandSlug);
            }

            Cache modelDetailsCache = cacheManager.getCache(CacheConfig.CACHE_VEHICLE_MODEL_DETAILS);
            if (modelDetailsCache != null && affectedModelIds != null) {
                for (Long modelId : affectedModelIds) {
                    modelDetailsCache.evict(modelId);
                }
            }
            log.info("Evicted cache for brand '{}' and {} model details", brandSlug, affectedModelIds != null ? affectedModelIds.size() : 0);
        } catch (Exception e) {
            log.warn("Cache eviction warning for brand '{}': {}", brandSlug, e.getMessage());
        }
    }

    private void evictAllCaches() {
        try {
            clearCache(CacheConfig.CACHE_VEHICLE_BRANDS);
            clearCache(CacheConfig.CACHE_VEHICLE_MODELS_BY_BRAND);
            clearCache(CacheConfig.CACHE_VEHICLE_MODEL_DETAILS);
            log.info("Evicted all vehicle catalogue caches");
        } catch (Exception e) {
            log.warn("Error clearing all caches: {}", e.getMessage());
        }
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
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

    public static class UpsertResult {
        private final boolean inserted;
        private final Long modelId;

        public UpsertResult(boolean inserted, Long modelId) {
            this.inserted = inserted;
            this.modelId = modelId;
        }

        public boolean isInserted() {
            return inserted;
        }

        public Long getModelId() {
            return modelId;
        }
    }
}
