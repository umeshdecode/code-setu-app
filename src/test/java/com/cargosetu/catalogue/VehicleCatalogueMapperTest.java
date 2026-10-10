package com.cargosetu.catalogue;

import com.cargosetu.client.trucksfloor.*;
import com.cargosetu.dto.VehicleBrandDto;
import com.cargosetu.dto.VehicleCatalogueResponseDto;
import com.cargosetu.dto.VehicleModelDto;
import com.cargosetu.entity.VehicleBrand;
import com.cargosetu.entity.VehicleModel;
import com.cargosetu.entity.VehicleModelImage;
import com.cargosetu.entity.VehicleSpecification;
import com.cargosetu.mapper.VehicleCatalogueMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class VehicleCatalogueMapperTest {

    private VehicleCatalogueMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new VehicleCatalogueMapper();
    }

    @Test
    void testCanonicalUnitConversions() {
        // Engine displacement
        assertThat(mapper.normalizeDisplacementCc(2179, "cc")).isEqualTo(2179);
        assertThat(mapper.normalizeDisplacementCc(2, "litres")).isEqualTo(2000);
        assertThat(mapper.normalizeDisplacementCc(null, "cc")).isNull();
        assertThat(mapper.normalizeDisplacementCc(0, "cc")).isNull();

        // Dimensions (wheelbase, length, etc.)
        assertThat(mapper.normalizeMm(3150, "mm")).isEqualTo(3150);
        assertThat(mapper.normalizeMm(5, "m")).isEqualTo(5000);
        assertThat(mapper.normalizeMm(186, "cm")).isEqualTo(1860);
        assertThat(mapper.normalizeMm(null, "mm")).isNull();

        // Weight / loading (kerb weight, payload, gvw)
        assertThat(mapper.normalizeWeightKg(1850, "kg")).isEqualTo(1850);
        assertThat(mapper.normalizeWeightKg(3, "tonne")).isEqualTo(3000);
        assertThat(mapper.normalizeWeightKg(null, "kg")).isNull();

        // Power
        assertThat(mapper.normalizePowerHp(100.0, "hp")).isEqualTo(100.0);
        Double fromKw = mapper.normalizePowerHp(75.0, "kW");
        assertThat(fromKw).isCloseTo(100.6, within(0.1));

        // Torque
        assertThat(mapper.normalizeTorqueNm(250.0, "Nm")).isEqualTo(250.0);

        // Speed
        assertThat(mapper.normalizeSpeedKmh(80.0, "km/h")).isEqualTo(80.0);
        Double fromMph = mapper.normalizeSpeedKmh(50.0, "mph");
        assertThat(fromMph).isCloseTo(80.5, within(0.1));
    }

    @Test
    void testMissingAndDynamicValuesRemainNullAndNotZero() {
        TrucksfloorProduct product = new TrucksfloorProduct();
        product.setSlug("test-truck");
        product.setVehicleName("Test Truck");

        TrucksfloorEngineDto engine = new TrucksfloorEngineDto();
        engine.setEngineType("Variable");
        engine.setEmissionNorm("Not Available");
        engine.setFuelTypeTxt("N/A");
        product.setEngine(engine);

        VehicleModel model = new VehicleModel();
        VehicleSpecification spec = mapper.toSpecificationEntity(product, model);

        // Assert all missing/unavailable fields are null, NOT zero
        assertThat(spec.getEngineType()).isNull();
        assertThat(spec.getEmissionNorm()).isNull();
        assertThat(spec.getFuelType()).isNull();
        assertThat(spec.getEngineCylinders()).isNull();
        assertThat(spec.getEngineDisplacementCc()).isNull();
        assertThat(spec.getMileageKmpl()).isNull();
        assertThat(spec.getFuelTankCapacityLitres()).isNull();
        assertThat(spec.getTorqueNm()).isNull();
        assertThat(spec.getEnginePowerHp()).isNull();
        assertThat(spec.getMaxSpeedKmh()).isNull();
    }

    @Test
    void testProductToEntityAndDtoMapping() {
        VehicleBrand brand = VehicleBrand.builder()
                .id(1L)
                .name("Tata")
                .slug("tata")
                .active(true)
                .build();

        TrucksfloorProduct product = new TrucksfloorProduct();
        product.setSlug("yodha-crew-cab");
        product.setVehicleName("Yodha Crew Cab");
        product.setBrandVehicleName("Tata Yodha Crew Cab");
        product.setPrimarySlug("yodha");
        product.setCategory("Pickups");
        product.setSubCategory("Commercial");
        product.setSeries("Yodha");

        TrucksfloorEngineDto engine = new TrucksfloorEngineDto();
        engine.setEngineDisplacement(2179);
        engine.setEngineDisplacementUnit("cc");
        engine.setEngineCylinders(4);
        engine.setFuelTypeTxt("Diesel");
        engine.setFuelTankCapacity(52);
        engine.setFuelTankCapacityUnit("litres");
        engine.setEnginePower(100.0);
        engine.setEnginePowerUnit("hp");
        engine.setTorque(250.0);
        engine.setTorqueUnit("Nm");
        product.setEngine(engine);

        TrucksfloorDimensionDto dim = new TrucksfloorDimensionDto();
        dim.setWheelbase(3150);
        dim.setOverallLength(5350);
        dim.setOverallWidth(1860);
        dim.setOverallHeight(1810);
        dim.setGroundClearance(210);
        product.setDimension(dim);

        TrucksfloorTransmissionLoadingDto tl = new TrucksfloorTransmissionLoadingDto();
        tl.setKerbWeight(1850);
        tl.setGvwGcw(2990);
        tl.setPayload(1140);
        product.setTransmissionLoading(tl);

        TrucksfloorPriceDto price = new TrucksfloorPriceDto();
        price.setMinPrice(new BigDecimal("990000.00"));
        price.setMaxPrice(new BigDecimal("1050000.00"));
        product.setPrice(price);

        // Map to entities
        VehicleModel model = mapper.toModelEntity(product, brand);
        model.setId(25L);
        VehicleSpecification spec = mapper.toSpecificationEntity(product, model);
        spec.setId(101L);

        VehicleModelImage img = VehicleModelImage.builder()
                .id(1L)
                .vehicleModel(model)
                .imageUrl("https://images.trucksfloor.com/yodha.jpg")
                .displayOrder(1)
                .build();

        // Convert to DTO
        VehicleCatalogueResponseDto responseDto = mapper.toCatalogueResponseDto(model, spec, List.of(img));

        assertThat(responseDto.getId()).isEqualTo(25L);
        assertThat(responseDto.getName()).isEqualTo("Yodha Crew Cab");
        assertThat(responseDto.getSlug()).isEqualTo("yodha-crew-cab");
        assertThat(responseDto.getBrand().getName()).isEqualTo("Tata");
        assertThat(responseDto.getBrand().getSlug()).isEqualTo("tata");
        assertThat(responseDto.getImages()).containsExactly("https://images.trucksfloor.com/yodha.jpg");

        // Verify spec fields matching Section 15.3
        assertThat(responseDto.getSpecifications().getEngineDisplacementCc()).isEqualTo(2179);
        assertThat(responseDto.getSpecifications().getEngineCylinders()).isEqualTo(4);
        assertThat(responseDto.getSpecifications().getFuelType()).isEqualTo("Diesel");
        assertThat(responseDto.getSpecifications().getFuelTankCapacityLitres()).isEqualTo(52);
        assertThat(responseDto.getSpecifications().getEnginePowerHp()).isEqualTo(100.0);
        assertThat(responseDto.getSpecifications().getTorqueNm()).isEqualTo(250.0);
        assertThat(responseDto.getSpecifications().getWheelbaseMm()).isEqualTo(3150);
        assertThat(responseDto.getSpecifications().getOverallLengthMm()).isEqualTo(5350);
        assertThat(responseDto.getSpecifications().getOverallWidthMm()).isEqualTo(1860);
        assertThat(responseDto.getSpecifications().getOverallHeightMm()).isEqualTo(1810);
        assertThat(responseDto.getSpecifications().getGroundClearanceMm()).isEqualTo(210);
        assertThat(responseDto.getSpecifications().getKerbWeightKg()).isEqualTo(1850);
        assertThat(responseDto.getSpecifications().getGvwKg()).isEqualTo(2990);
        assertThat(responseDto.getSpecifications().getPayloadKg()).isEqualTo(1140);
    }
}
