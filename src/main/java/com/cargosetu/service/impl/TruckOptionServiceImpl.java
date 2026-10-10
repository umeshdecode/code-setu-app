package com.cargosetu.service.impl;

import com.cargosetu.dto.TruckOptionsDto;
import com.cargosetu.entity.TruckOption;
import com.cargosetu.repository.TruckOptionRepository;
import com.cargosetu.service.TruckOptionService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TruckOptionServiceImpl implements TruckOptionService {

    private final TruckOptionRepository repository;

    @PostConstruct
    public void onInit() {
        try {
            initDefaultTruckOptionsIfEmpty();
        } catch (Exception e) {
            log.error("Error initializing default truck options: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TruckOptionsDto> getPublicTruckOptions(String truckType) {
        List<TruckOption> activeOptions = repository.findByActiveTrueAndIsDeletedFalse();
        if (activeOptions.isEmpty()) {
            initDefaultTruckOptionsIfEmpty();
            activeOptions = repository.findByActiveTrueAndIsDeletedFalse();
        }

        List<TruckOptionsDto> dtoList = activeOptions.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        if (truckType != null && !truckType.trim().isEmpty()) {
            String filter = truckType.trim().toUpperCase();
            return dtoList.stream()
                    .filter(opt -> opt.getTruckType().equalsIgnoreCase(filter) || opt.getDisplayName().toUpperCase().contains(filter))
                    .collect(Collectors.toList());
        }

        return dtoList;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TruckOptionsDto> getAllTruckOptions() {
        return repository.findByIsDeletedFalse().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TruckOptionsDto getTruckOptionById(Long id) {
        TruckOption entity = repository.findById(id)
                .filter(opt -> !opt.getIsDeleted())
                .orElseThrow(() -> new RuntimeException("Truck option not found with id: " + id));
        return toDto(entity);
    }

    @Override
    @Transactional
    public TruckOptionsDto createTruckOption(TruckOptionsDto dto) {
        if (dto.getTruckType() == null || dto.getTruckType().trim().isEmpty()) {
            throw new IllegalArgumentException("Truck type is mandatory");
        }
        if (repository.existsByTruckTypeIgnoreCaseAndIsDeletedFalse(dto.getTruckType().trim())) {
            throw new IllegalArgumentException("Truck type already exists: " + dto.getTruckType());
        }

        TruckOption entity = TruckOption.builder()
                .truckType(dto.getTruckType().trim().toUpperCase())
                .displayName(dto.getDisplayName() != null ? dto.getDisplayName().trim() : dto.getTruckType().trim())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .isDeleted(false)
                .bodyTypes(dto.getBodyTypes() != null ? new ArrayList<>(dto.getBodyTypes()) : new ArrayList<>())
                .availableLengthsFeet(dto.getAvailableLengthsFeet() != null ? new ArrayList<>(dto.getAvailableLengthsFeet()) : new ArrayList<>())
                .availableWheelers(dto.getAvailableWheelers() != null ? new ArrayList<>(dto.getAvailableWheelers()) : new ArrayList<>())
                .availablePassingTons(dto.getAvailablePassingTons() != null ? new ArrayList<>(dto.getAvailablePassingTons()) : new ArrayList<>())
                .build();

        TruckOption saved = repository.save(entity);
        log.info("Created truck option: {}", saved.getTruckType());
        return toDto(saved);
    }

    @Override
    @Transactional
    public TruckOptionsDto updateTruckOption(Long id, TruckOptionsDto dto) {
        TruckOption entity = repository.findById(id)
                .filter(opt -> !opt.getIsDeleted())
                .orElseThrow(() -> new RuntimeException("Truck option not found with id: " + id));

        if (dto.getDisplayName() != null) {
            entity.setDisplayName(dto.getDisplayName().trim());
        }
        if (dto.getActive() != null) {
            entity.setActive(dto.getActive());
        }
        if (dto.getBodyTypes() != null) {
            entity.setBodyTypes(new ArrayList<>(dto.getBodyTypes()));
        }
        if (dto.getAvailableLengthsFeet() != null) {
            entity.setAvailableLengthsFeet(new ArrayList<>(dto.getAvailableLengthsFeet()));
        }
        if (dto.getAvailableWheelers() != null) {
            entity.setAvailableWheelers(new ArrayList<>(dto.getAvailableWheelers()));
        }
        if (dto.getAvailablePassingTons() != null) {
            entity.setAvailablePassingTons(new ArrayList<>(dto.getAvailablePassingTons()));
        }

        TruckOption updated = repository.save(entity);
        log.info("Updated truck option id {}: {}", id, updated.getTruckType());
        return toDto(updated);
    }

    @Override
    @Transactional
    public void deleteTruckOption(Long id) {
        TruckOption entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Truck option not found with id: " + id));

        entity.setIsDeleted(true);
        entity.setActive(false);
        repository.save(entity);
        log.info("Deleted (soft) truck option id: {}", id);
    }

    @Override
    @Transactional
    public void initDefaultTruckOptionsIfEmpty() {
        if (repository.count() > 0) {
            return;
        }
        log.info("Seeding initial default truck options into database...");

        List<TruckOption> defaults = List.of(
                TruckOption.builder()
                        .truckType("LCV")
                        .displayName("LCV (Light Commercial Vehicle)")
                        .active(true)
                        .isDeleted(false)
                        .bodyTypes(List.of("LCV_OPEN", "LCV_CONTAINER"))
                        .availableLengthsFeet(List.of("7 Feet", "8 Feet", "9 Feet", "10 Feet", "12 Feet", "14 Feet"))
                        .availableWheelers(List.of("4 Wheeler", "6 Wheeler"))
                        .availablePassingTons(List.of("1-2 Ton", "2-3.5 Ton", "3.5-5 Ton", "5-7.5 Ton"))
                        .build(),

                TruckOption.builder()
                        .truckType("OPEN_TRUCK")
                        .displayName("Open Truck / Flatbed / High Deck")
                        .active(true)
                        .isDeleted(false)
                        .bodyTypes(List.of("OPEN_FULL_BODY", "OPEN_HALF_BODY", "OPEN_HIGH_BED", "FLATBED"))
                        .availableLengthsFeet(List.of("14 Feet", "17 Feet", "19 Feet", "20 Feet", "22 Feet", "24 Feet", "32 Feet"))
                        .availableWheelers(List.of("6 Wheeler", "10 Wheeler", "12 Wheeler", "14 Wheeler", "16 Wheeler", "18 Wheeler", "22 Wheeler"))
                        .availablePassingTons(List.of("7.5-10 Ton", "10-15 Ton", "15-20 Ton", "20-24 Ton", "24-25 Ton", "25-30 Ton", "30+ Ton"))
                        .build(),

                TruckOption.builder()
                        .truckType("CONTAINER")
                        .displayName("Closed Container Truck")
                        .active(true)
                        .isDeleted(false)
                        .bodyTypes(List.of("CLOSED_CONTAINER", "HIGH_CUBE_CONTAINER", "INSULATED_CONTAINER"))
                        .availableLengthsFeet(List.of("17 Feet", "19 Feet", "20 Feet", "22 Feet", "24 Feet", "28 Feet", "32 Feet SXL", "32 Feet MXL", "40 Feet"))
                        .availableWheelers(List.of("6 Wheeler", "10 Wheeler", "12 Wheeler", "14 Wheeler", "16 Wheeler"))
                        .availablePassingTons(List.of("7.5-10 Ton", "10-15 Ton", "15-20 Ton", "20-24 Ton", "24-25 Ton", "25-30 Ton"))
                        .build(),

                TruckOption.builder()
                        .truckType("TIPPER")
                        .displayName("Tipper / Dumper")
                        .active(true)
                        .isDeleted(false)
                        .bodyTypes(List.of("TIPPER_BOX_BODY", "TIPPER_ROCK_BODY"))
                        .availableLengthsFeet(List.of("Standard Box"))
                        .availableWheelers(List.of("6 Wheeler", "10 Wheeler", "12 Wheeler", "14 Wheeler", "16 Wheeler"))
                        .availablePassingTons(List.of("10-15 Ton", "15-20 Ton", "20-25 Ton", "25-35 Ton"))
                        .build(),

                TruckOption.builder()
                        .truckType("TANKER")
                        .displayName("Tanker")
                        .active(true)
                        .isDeleted(false)
                        .bodyTypes(List.of("STAINLESS_STEEL_TANKER", "ALUMINUM_TANKER", "CHEMICAL_TANKER", "PETROLEUM_TANKER", "MILK_TANKER", "WATER_TANKER"))
                        .availableLengthsFeet(List.of("Standard Tank"))
                        .availableWheelers(List.of("6 Wheeler", "10 Wheeler", "12 Wheeler", "14 Wheeler"))
                        .availablePassingTons(List.of("10-15 Ton", "15-20 Ton", "20-25 Ton", "25-30 Ton"))
                        .build(),

                TruckOption.builder()
                        .truckType("TRAILER")
                        .displayName("Trailer")
                        .active(true)
                        .isDeleted(false)
                        .bodyTypes(List.of("FLATBED_TRAILER", "LOWBED_TRAILER", "HIGHBED_TRAILER", "SKELETAL_TRAILER"))
                        .availableLengthsFeet(List.of("40 Feet", "45 Feet", "50 Feet"))
                        .availableWheelers(List.of("18 Wheeler", "22 Wheeler", "26 Wheeler"))
                        .availablePassingTons(List.of("30-35 Ton", "35-40 Ton", "40-55 Ton"))
                        .build(),

                TruckOption.builder()
                        .truckType("REEFER")
                        .displayName("Refrigerated / Reefer")
                        .active(true)
                        .isDeleted(false)
                        .bodyTypes(List.of("REFRIGERATED_CONTAINER", "COLD_VAN"))
                        .availableLengthsFeet(List.of("14 Feet", "17 Feet", "20 Feet", "24 Feet", "32 Feet"))
                        .availableWheelers(List.of("6 Wheeler", "10 Wheeler", "12 Wheeler"))
                        .availablePassingTons(List.of("5-10 Ton", "10-15 Ton", "15-20 Ton"))
                        .build()
        );

        repository.saveAll(defaults);
    }

    private TruckOptionsDto toDto(TruckOption entity) {
        if (entity == null) {
            return null;
        }
        return TruckOptionsDto.builder()
                .id(entity.getId())
                .truckType(entity.getTruckType())
                .displayName(entity.getDisplayName())
                .active(entity.getActive())
                .bodyTypes(entity.getBodyTypes() != null ? new ArrayList<>(entity.getBodyTypes()) : new ArrayList<>())
                .availableLengthsFeet(entity.getAvailableLengthsFeet() != null ? new ArrayList<>(entity.getAvailableLengthsFeet()) : new ArrayList<>())
                .availableWheelers(entity.getAvailableWheelers() != null ? new ArrayList<>(entity.getAvailableWheelers()) : new ArrayList<>())
                .availablePassingTons(entity.getAvailablePassingTons() != null ? new ArrayList<>(entity.getAvailablePassingTons()) : new ArrayList<>())
                .build();
    }
}
