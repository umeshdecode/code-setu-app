package com.cargosetu.mapper;

import com.cargosetu.client.trucksfloor.*;
import com.cargosetu.dto.*;
import com.cargosetu.entity.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class VehicleCatalogueMapper {

    // ==========================================
    // Entity -> DTO Mappers
    // ==========================================

    public VehicleBrandDto toBrandDto(VehicleBrand brand) {
        if (brand == null) {
            return null;
        }
        return VehicleBrandDto.builder()
                .id(brand.getId())
                .name(brand.getName())
                .slug(brand.getSlug())
                .build();
    }

    public VehicleModelDto toModelDto(VehicleModel model) {
        if (model == null) {
            return null;
        }
        return VehicleModelDto.builder()
                .id(model.getId())
                .name(model.getName())
                .slug(model.getSlug())
                .build();
    }

    public VehicleSpecificationDto toSpecificationDto(VehicleSpecification spec) {
        if (spec == null) {
            return null;
        }
        return VehicleSpecificationDto.builder()
                .engineType(spec.getEngineType())
                .engineCylinders(spec.getEngineCylinders())
                .engineDisplacementCc(spec.getEngineDisplacementCc())
                .fuelType(spec.getFuelType())
                .emissionNorm(spec.getEmissionNorm())
                .mileageKmpl(spec.getMileageKmpl())
                .fuelTankCapacityLitres(spec.getFuelTankCapacityLitres())
                .torqueNm(spec.getTorqueNm())
                .enginePowerHp(spec.getEnginePowerHp())
                .engineRpm(spec.getEngineRpm())
                .maxSpeedKmh(spec.getMaxSpeedKmh())
                .gradeability(spec.getGradeability())
                .turningRadiusMm(spec.getTurningRadiusMm())
                .wheelbaseMm(spec.getWheelbaseMm())
                .deckLengthMm(spec.getDeckLengthMm())
                .groundClearanceMm(spec.getGroundClearanceMm())
                .overallWidthMm(spec.getOverallWidthMm())
                .overallHeightMm(spec.getOverallHeightMm())
                .overallLengthMm(spec.getOverallLengthMm())
                .kerbWeightKg(spec.getKerbWeightKg())
                .gvwKg(spec.getGvwKg())
                .payloadKg(spec.getPayloadKg())
                .gearbox(spec.getGearbox())
                .axleConfig(spec.getAxleConfig())
                .frontAxle(spec.getFrontAxle())
                .rearAxle(spec.getRearAxle())
                .frontSuspension(spec.getFrontSuspension())
                .rearSuspension(spec.getRearSuspension())
                .brakeType(spec.getBrakeType())
                .abs(spec.getAbs())
                .chassisType(spec.getChassisType())
                .cabinType(spec.getCabinType())
                .bodyOption(spec.getBodyOption())
                .seatingCapacity(spec.getSeatingCapacity())
                .tubelessTyres(spec.getTubelessTyres())
                .frontTyre(spec.getFrontTyre())
                .rearTyre(spec.getRearTyre())
                .numberOfTyres(spec.getNumberOfTyres())
                .minPrice(spec.getMinPrice())
                .maxPrice(spec.getMaxPrice())
                .build();
    }

    public VehicleCatalogueResponseDto toCatalogueResponseDto(VehicleModel model,
                                                              VehicleSpecification spec,
                                                              List<VehicleModelImage> images) {
        if (model == null) {
            return null;
        }

        List<String> imageUrls = (images != null)
                ? images.stream().map(VehicleModelImage::getImageUrl).collect(Collectors.toList())
                : Collections.emptyList();

        return VehicleCatalogueResponseDto.builder()
                .id(model.getId())
                .brand(toBrandDto(model.getBrand()))
                .name(model.getName())
                .slug(model.getSlug())
                .category(model.getCategory())
                .subCategory(model.getSubCategory())
                .seriesName(model.getSeriesName())
                .brochureUrl(model.getBrochureUrl())
                .images(imageUrls)
                .specifications(toSpecificationDto(spec))
                .build();
    }

    // ==========================================
    // External Product -> JPA Entity Mappers
    // ==========================================

    public VehicleModel toModelEntity(TrucksfloorProduct product, VehicleBrand brand) {
        VehicleModel model = new VehicleModel();
        model.setBrand(brand);
        model.setActive(true);
        model.setSource("trucksfloor");
        updateModelFromProduct(model, product);
        return model;
    }

    public void updateModelFromProduct(VehicleModel model, TrucksfloorProduct product) {
        model.setName(product.resolveModelName());
        model.setSlug(product.getSlug());
        model.setBrandVehicleName(product.getBrandVehicleName());
        model.setPrimarySlug(product.getPrimarySlug());
        model.setCategory(product.getCategory());
        model.setSubCategory(product.getSubCategory());
        model.setSeriesName(product.getSeries());
        model.setSeriesSlug(product.getSeriesSlug());
        model.setBrochureUrl(product.extractBrochureUrl());
        model.setSourceProductId(product.resolveProductId());
    }

    public VehicleSpecification toSpecificationEntity(TrucksfloorProduct product, VehicleModel model) {
        VehicleSpecification spec = new VehicleSpecification();
        spec.setVehicleModel(model);
        updateSpecificationFromProduct(spec, product);
        return spec;
    }

    public void updateSpecificationFromProduct(VehicleSpecification spec, TrucksfloorProduct product) {
        spec.setLastSyncedAt(LocalDateTime.now());

        TrucksfloorEngineDto engine = product.getEngine();
        if (engine != null) {
            spec.setEngineType(cleanString(engine.getEngineType()));
            spec.setEngineCylinders(engine.getEngineCylinders());
            spec.setEngineDisplacementCc(normalizeDisplacementCc(engine.getEngineDisplacement(), engine.getEngineDisplacementUnit()));

            String fuelType = null;
            if (engine.getFuelType() != null && !engine.getFuelType().isEmpty()) {
                fuelType = String.join(", ", engine.getFuelType());
            } else if (engine.getFuelTypeTxt() != null && !engine.getFuelTypeTxt().trim().isEmpty()) {
                fuelType = engine.getFuelTypeTxt().trim();
            }
            spec.setFuelType(cleanString(fuelType));

            spec.setEmissionNorm(cleanString(engine.getEmissionNorm()));
            spec.setMileageKmpl(normalizeMileageKmpl(engine.getMileage()));
            spec.setFuelTankCapacityLitres(normalizeFuelTankLitres(engine.getFuelTankCapacity(), engine.getFuelTankCapacityUnit()));
            spec.setTorqueNm(normalizeTorqueNm(engine.getTorque(), engine.getTorqueUnit()));
            spec.setEnginePowerHp(normalizePowerHp(engine.getEnginePower(), engine.getEnginePowerUnit()));
            spec.setEngineRpm(engine.getEngineRpm());
            spec.setMaxSpeedKmh(normalizeSpeedKmh(engine.getMaxSpeed(), engine.getMaxSpeedUnit()));
            spec.setGradeability(engine.getGradeability());
        }

        TrucksfloorDimensionDto dim = product.getDimension();
        if (dim != null) {
            spec.setTurningRadiusMm(normalizeMm(dim.getTurningRadius(), dim.getTurningRadiusUnit()));
            spec.setWheelbaseMm(normalizeMm(dim.getWheelbase(), dim.getWheelbaseUnit()));
            spec.setDeckLengthMm(normalizeMm(dim.getDeckLength(), dim.getDeckLengthUnit()));
            spec.setGroundClearanceMm(normalizeMm(dim.getGroundClearance(), dim.getGroundClearanceUnit()));
            spec.setOverallWidthMm(normalizeMm(dim.getOverallWidth(), dim.getOverallWidthUnit()));
            spec.setOverallHeightMm(normalizeMm(dim.getOverallHeight(), dim.getOverallHeightUnit()));
            spec.setOverallLengthMm(normalizeMm(dim.getOverallLength(), dim.getOverallLengthUnit()));
        }

        TrucksfloorTransmissionLoadingDto tl = product.getTransmissionLoading();
        if (tl != null) {
            spec.setKerbWeightKg(normalizeWeightKg(tl.getKerbWeight(), tl.getKerbWeightUnit()));
            spec.setGvwKg(normalizeWeightKg(tl.getGvwGcw(), tl.getGvwGcwUnit()));
            spec.setPayloadKg(normalizeWeightKg(tl.getPayload(), tl.getPayloadUnit()));
            spec.setGearbox(cleanString(tl.getGearbox()));
        }

        TrucksfloorBrakeSuspensionDto bs = product.getBrakeSuspension();
        if (bs != null) {
            spec.setAxleConfig(cleanString(bs.getAxleConfig()));
            spec.setFrontAxle(cleanString(bs.getFrontAxle()));
            spec.setRearAxle(cleanString(bs.getRearAxle()));
            spec.setFrontSuspension(cleanString(bs.getFrontSuspension()));
            spec.setRearSuspension(cleanString(bs.getRearSuspension()));
            spec.setBrakeType(cleanString(bs.getBrakeType()));
            spec.setAbs(bs.getAbs());
        }

        TrucksfloorBodyOptionCabinTypeDto bo = product.getBodyOptionCabinType();
        if (bo != null) {
            spec.setChassisType(cleanString(bo.getChassisType()));
            spec.setCabinType(cleanString(bo.getCabinType()));
            spec.setBodyOption(cleanString(bo.getBodyOption()));
            spec.setSeatingCapacity(bo.getSeatingCapacity());
        }

        TrucksfloorTyresDto tyres = product.getTyres();
        if (tyres != null) {
            spec.setTubelessTyres(tyres.getTubelessTyres());
            spec.setFrontTyre(cleanString(tyres.getFrontTyre()));
            spec.setRearTyre(cleanString(tyres.getRearTyre()));
            spec.setNumberOfTyres(tyres.getNumberOfTyres());
        }

        TrucksfloorPriceDto price = product.getPrice();
        if (price != null) {
            spec.setMinPrice(price.getMinPrice());
            spec.setMaxPrice(price.getMaxPrice());
        }
    }

    // ==========================================
    // Canonical Unit Conversion Helpers
    // ==========================================

    public Integer normalizeDisplacementCc(Integer value, String unit) {
        if (value == null || value <= 0) {
            return null;
        }
        if (unit != null) {
            String u = unit.trim().toLowerCase();
            if (u.contains("litre") || u.equals("l")) {
                return value * 1000;
            }
        }
        return value;
    }

    public Integer normalizeMm(Integer value, String unit) {
        if (value == null || value <= 0) {
            return null;
        }
        if (unit != null) {
            String u = unit.trim().toLowerCase();
            if (u.equals("m") || u.equals("meter") || u.equals("meters")) {
                return value * 1000;
            }
            if (u.equals("cm")) {
                return value * 10;
            }
            if (u.equals("ft") || u.equals("feet")) {
                return (int) Math.round(value * 304.8);
            }
            if (u.equals("in") || u.equals("inch") || u.equals("inches")) {
                return (int) Math.round(value * 25.4);
            }
        }
        return value;
    }

    public Integer normalizeWeightKg(Integer value, String unit) {
        if (value == null || value <= 0) {
            return null;
        }
        if (unit != null) {
            String u = unit.trim().toLowerCase();
            if (u.contains("ton") || u.equals("t")) {
                return value * 1000;
            }
            if (u.contains("lb") || u.contains("pound")) {
                return (int) Math.round(value * 0.453592);
            }
        }
        return value;
    }

    public Integer normalizeFuelTankLitres(Integer value, String unit) {
        if (value == null || value <= 0) {
            return null;
        }
        return value;
    }

    public Double normalizePowerHp(Double value, String unit) {
        if (value == null || value <= 0) {
            return null;
        }
        if (unit != null) {
            String u = unit.trim().toLowerCase();
            if (u.contains("kw")) {
                return Math.round((value * 1.34102) * 10.0) / 10.0;
            }
        }
        return value;
    }

    public Double normalizeTorqueNm(Double value, String unit) {
        if (value == null || value <= 0) {
            return null;
        }
        if (unit != null) {
            String u = unit.trim().toLowerCase();
            if (u.contains("kgm") || u.contains("kg-m")) {
                return Math.round((value * 9.80665) * 10.0) / 10.0;
            }
        }
        return value;
    }

    public Double normalizeSpeedKmh(Double value, String unit) {
        if (value == null || value <= 0) {
            return null;
        }
        if (unit != null) {
            String u = unit.trim().toLowerCase();
            if (u.contains("mph")) {
                return Math.round((value * 1.60934) * 10.0) / 10.0;
            }
        }
        return value;
    }

    public Double normalizeMileageKmpl(Double value) {
        if (value == null || value <= 0) {
            return null;
        }
        return value;
    }

    private String cleanString(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()
                || trimmed.equalsIgnoreCase("not available")
                || trimmed.equalsIgnoreCase("variable")
                || trimmed.equalsIgnoreCase("n/a")
                || trimmed.equalsIgnoreCase("null")) {
            return null;
        }
        return trimmed;
    }
}
