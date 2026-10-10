package com.cargosetu.catalogue;

import com.cargosetu.config.CacheConfig;
import com.cargosetu.entity.VehicleBrand;
import com.cargosetu.entity.VehicleModel;
import com.cargosetu.repository.VehicleBrandRepository;
import com.cargosetu.repository.VehicleModelImageRepository;
import com.cargosetu.repository.VehicleModelRepository;
import com.cargosetu.repository.VehicleSpecificationRepository;
import com.cargosetu.service.VehicleCatalogueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
class VehicleCatalogueCacheTest {

    @Autowired
    private VehicleCatalogueService catalogueService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private VehicleBrandRepository brandRepository;

    @MockBean
    private VehicleModelRepository modelRepository;

    @MockBean
    private VehicleSpecificationRepository specRepository;

    @MockBean
    private VehicleModelImageRepository imageRepository;

    @MockBean
    private com.cargosetu.client.cardekho.CardekhoClient cardekhoClient;

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name ->
                Objects.requireNonNull(cacheManager.getCache(name)).clear()
        );
    }

    @Test
    void testBrandsCacheHitAndEviction() {
        VehicleBrand brand = VehicleBrand.builder()
                .id(1L)
                .name("Tata")
                .slug("tata")
                .active(true)
                .build();

        when(brandRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(brand));

        // First call - should invoke repo
        catalogueService.getBrands();
        verify(brandRepository, times(1)).findByActiveTrueOrderByNameAsc();

        // Second call - should hit Caffeine cache
        catalogueService.getBrands();
        verify(brandRepository, times(1)).findByActiveTrueOrderByNameAsc();

        // Evict cache
        Objects.requireNonNull(cacheManager.getCache(CacheConfig.CACHE_VEHICLE_BRANDS)).clear();

        // Third call - should hit repo again
        catalogueService.getBrands();
        verify(brandRepository, times(2)).findByActiveTrueOrderByNameAsc();
    }

    @Test
    void testModelsByBrandCacheHit() {
        VehicleModel model = VehicleModel.builder()
                .id(1L)
                .name("Intra V30")
                .slug("intra-v30")
                .active(true)
                .build();

        List<VehicleModel> tenModels = java.util.Collections.nCopies(10, model);
        when(modelRepository.findByBrandSlugAndActiveTrueOrderByNameAsc("tata")).thenReturn(tenModels);

        // First call
        catalogueService.getModelsByBrand("tata");
        verify(modelRepository, times(1)).findByBrandSlugAndActiveTrueOrderByNameAsc("tata");

        // Second call - cache hit
        catalogueService.getModelsByBrand("tata");
        verify(modelRepository, times(1)).findByBrandSlugAndActiveTrueOrderByNameAsc("tata");
    }
}
