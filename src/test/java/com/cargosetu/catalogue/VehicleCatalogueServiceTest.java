package com.cargosetu.catalogue;

import com.cargosetu.dto.VehicleBrandDto;
import com.cargosetu.dto.VehicleCatalogueResponseDto;
import com.cargosetu.dto.VehicleModelDto;
import com.cargosetu.entity.VehicleBrand;
import com.cargosetu.entity.VehicleModel;
import com.cargosetu.entity.VehicleSpecification;
import com.cargosetu.mapper.VehicleCatalogueMapper;
import com.cargosetu.repository.VehicleBrandRepository;
import com.cargosetu.repository.VehicleModelImageRepository;
import com.cargosetu.repository.VehicleModelRepository;
import com.cargosetu.repository.VehicleSpecificationRepository;
import com.cargosetu.service.VehicleCatalogueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleCatalogueServiceTest {

    @Mock
    private VehicleBrandRepository brandRepository;

    @Mock
    private VehicleModelRepository modelRepository;

    @Mock
    private VehicleSpecificationRepository specRepository;

    @Mock
    private VehicleModelImageRepository imageRepository;

    @Mock
    private com.cargosetu.client.cardekho.CardekhoClient cardekhoClient;

    @Spy
    private VehicleCatalogueMapper mapper = new VehicleCatalogueMapper();

    @InjectMocks
    private VehicleCatalogueService catalogueService;

    private VehicleBrand brand;
    private VehicleModel model;
    private VehicleSpecification spec;

    @BeforeEach
    void setUp() {
        brand = VehicleBrand.builder()
                .id(1L)
                .name("Tata")
                .slug("tata")
                .active(true)
                .build();

        model = VehicleModel.builder()
                .id(25L)
                .brand(brand)
                .name("Yodha Crew Cab")
                .slug("yodha-crew-cab")
                .active(true)
                .build();

        spec = VehicleSpecification.builder()
                .id(100L)
                .vehicleModel(model)
                .engineDisplacementCc(2179)
                .engineCylinders(4)
                .fuelType("Diesel")
                .build();
    }

    @Test
    void testGetBrands() {
        when(brandRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(brand));

        List<VehicleBrandDto> brands = catalogueService.getBrands();

        assertThat(brands).hasSize(1);
        assertThat(brands.get(0).getName()).isEqualTo("Tata");
        assertThat(brands.get(0).getSlug()).isEqualTo("tata");
    }

    @Test
    void testGetModelsByBrand() {
        List<VehicleModel> tenModels = Collections.nCopies(10, model);
        when(modelRepository.findByBrandSlugAndActiveTrueOrderByNameAsc("tata")).thenReturn(tenModels);

        List<VehicleModelDto> models = catalogueService.getModelsByBrand("tata");

        assertThat(models).hasSize(10);
        assertThat(models.get(0).getName()).isEqualTo("Yodha Crew Cab");
        assertThat(models.get(0).getSlug()).isEqualTo("yodha-crew-cab");
    }

    @Test
    void testGetModelDetailsFound() {
        when(modelRepository.findById(25L)).thenReturn(Optional.of(model));
        when(specRepository.findByVehicleModel(model)).thenReturn(Optional.of(spec));
        when(imageRepository.findByVehicleModelOrderByDisplayOrderAsc(model)).thenReturn(Collections.emptyList());

        VehicleCatalogueResponseDto details = catalogueService.getModelDetails(25L);

        assertThat(details).isNotNull();
        assertThat(details.getId()).isEqualTo(25L);
        assertThat(details.getName()).isEqualTo("Yodha Crew Cab");
        assertThat(details.getSpecifications().getEngineDisplacementCc()).isEqualTo(2179);
    }

    @Test
    void testGetModelDetailsNotFound() {
        when(modelRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogueService.getModelDetails(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void testSearchModels() {
        when(modelRepository.findByNameContainingIgnoreCaseAndActiveTrue("intra")).thenReturn(List.of(
                VehicleModel.builder().id(1L).name("Intra V30").slug("intra-v30").build()
        ));

        List<VehicleModelDto> results = catalogueService.searchModels("intra");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Intra V30");
    }
}
