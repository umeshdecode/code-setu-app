package com.cargosetu.catalogue;

import com.cargosetu.client.trucksfloor.TrucksfloorClient;
import com.cargosetu.client.trucksfloor.TrucksfloorProduct;
import com.cargosetu.client.trucksfloor.TrucksfloorResponse;
import com.cargosetu.config.CacheConfig;
import com.cargosetu.config.TrucksfloorProperties;
import com.cargosetu.dto.VehicleCatalogueSyncResultDto;
import com.cargosetu.entity.VehicleBrand;
import com.cargosetu.entity.VehicleModel;
import com.cargosetu.entity.VehicleSpecification;
import com.cargosetu.mapper.VehicleCatalogueMapper;
import com.cargosetu.repository.VehicleBrandRepository;
import com.cargosetu.repository.VehicleModelImageRepository;
import com.cargosetu.repository.VehicleModelRepository;
import com.cargosetu.repository.VehicleSpecificationRepository;
import com.cargosetu.service.VehicleCatalogueSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleCatalogueSyncServiceTest {

    @Mock
    private TrucksfloorClient client;

    @Mock
    private VehicleBrandRepository brandRepository;

    @Mock
    private VehicleModelRepository modelRepository;

    @Mock
    private VehicleSpecificationRepository specRepository;

    @Mock
    private VehicleModelImageRepository imageRepository;

    @Spy
    private VehicleCatalogueMapper mapper = new VehicleCatalogueMapper();

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @Spy
    private TrucksfloorProperties properties = new TrucksfloorProperties();

    @InjectMocks
    private VehicleCatalogueSyncService syncService;

    private VehicleBrand tataBrand;

    @BeforeEach
    void setUp() {
        tataBrand = VehicleBrand.builder()
                .id(1L)
                .name("Tata")
                .slug("tata")
                .active(true)
                .source("trucksfloor")
                .build();

        lenient().when(brandRepository.findBySlug("tata")).thenReturn(Optional.of(tataBrand));
        lenient().when(cacheManager.getCache(anyString())).thenReturn(cache);
    }

    @Test
    void testPaginationAllPagesProcessedAndLastPageRespected() {
        // Page 1 of 2
        TrucksfloorProduct product1 = new TrucksfloorProduct();
        product1.setSlug("ace-gold");
        product1.setVehicleName("Ace Gold");

        TrucksfloorResponse page1 = new TrucksfloorResponse();
        page1.setCurrentPage(1);
        page1.setLastPage(2);
        page1.setData(List.of(product1));

        // Page 2 of 2
        TrucksfloorProduct product2 = new TrucksfloorProduct();
        product2.setSlug("intra-v30");
        product2.setVehicleName("Intra V30");

        TrucksfloorResponse page2 = new TrucksfloorResponse();
        page2.setCurrentPage(2);
        page2.setLastPage(2);
        page2.setData(List.of(product2));

        when(client.getTrucks("tata", 1)).thenReturn(page1);
        when(client.getTrucks("tata", 2)).thenReturn(page2);

        when(modelRepository.findByBrandAndSlug(eq(tataBrand), anyString())).thenReturn(Optional.empty());
        when(modelRepository.save(any(VehicleModel.class))).thenAnswer(inv -> {
            VehicleModel m = inv.getArgument(0);
            m.setId(10L);
            return m;
        });
        when(specRepository.findByVehicleModel(any())).thenReturn(Optional.empty());

        VehicleCatalogueSyncResultDto result = syncService.syncBrand("tata");

        assertThat(result.getBrand()).isEqualTo("tata");
        assertThat(result.getPagesProcessed()).isEqualTo(2);
        assertThat(result.getProductsProcessed()).isEqualTo(2);
        assertThat(result.getInserted()).isEqualTo(2);
        assertThat(result.getUpdated()).isEqualTo(0);
        assertThat(result.getFailed()).isEqualTo(0);

        // Verify it never calls page 3
        verify(client, times(1)).getTrucks("tata", 1);
        verify(client, times(1)).getTrucks("tata", 2);
        verify(client, never()).getTrucks("tata", 3);
    }

    @Test
    void testIdempotentUpsertUpdatesExistingModelWithoutDuplicates() {
        TrucksfloorProduct product = new TrucksfloorProduct();
        product.setSlug("yodha-crew-cab");
        product.setVehicleName("Yodha Crew Cab Updated");

        TrucksfloorResponse response = new TrucksfloorResponse();
        response.setCurrentPage(1);
        response.setLastPage(1);
        response.setData(List.of(product));

        when(client.getTrucks("tata", 1)).thenReturn(response);

        // Model already exists
        VehicleModel existingModel = VehicleModel.builder()
                .id(25L)
                .brand(tataBrand)
                .name("Yodha Crew Cab")
                .slug("yodha-crew-cab")
                .build();
        when(modelRepository.findByBrandAndSlug(tataBrand, "yodha-crew-cab")).thenReturn(Optional.of(existingModel));
        when(modelRepository.save(any(VehicleModel.class))).thenReturn(existingModel);

        VehicleSpecification existingSpec = VehicleSpecification.builder()
                .id(50L)
                .vehicleModel(existingModel)
                .build();
        when(specRepository.findByVehicleModel(existingModel)).thenReturn(Optional.of(existingSpec));

        VehicleCatalogueSyncResultDto result = syncService.syncBrand("tata");

        assertThat(result.getInserted()).isEqualTo(0);
        assertThat(result.getUpdated()).isEqualTo(1);
        assertThat(result.getFailed()).isEqualTo(0);

        // Model was updated in place, not inserted
        verify(modelRepository).save(existingModel);
        assertThat(existingModel.getName()).isEqualTo("Yodha Crew Cab Updated");
    }
}
