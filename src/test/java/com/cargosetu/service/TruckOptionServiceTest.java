package com.cargosetu.service;

import com.cargosetu.dto.TruckOptionsDto;
import com.cargosetu.entity.TruckOption;
import com.cargosetu.repository.TruckOptionRepository;
import com.cargosetu.service.impl.TruckOptionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TruckOptionServiceTest {

    @Mock
    private TruckOptionRepository repository;

    @InjectMocks
    private TruckOptionServiceImpl truckOptionService;

    private TruckOption lcvOption;

    @BeforeEach
    void setUp() {
        lcvOption = TruckOption.builder()
                .id(1L)
                .truckType("LCV")
                .displayName("LCV (Light Commercial Vehicle)")
                .active(true)
                .isDeleted(false)
                .bodyTypes(new ArrayList<>(List.of("LCV_OPEN", "LCV_CONTAINER")))
                .availableLengthsFeet(new ArrayList<>(List.of("14 Feet")))
                .availableWheelers(new ArrayList<>(List.of("4 Wheeler", "6 Wheeler")))
                .availablePassingTons(new ArrayList<>(List.of("1-2 Ton", "5-7.5 Ton")))
                .build();
    }

    @Test
    @DisplayName("getPublicTruckOptions returns active options")
    void testGetPublicTruckOptions() {
        when(repository.findByActiveTrueAndIsDeletedFalse()).thenReturn(List.of(lcvOption));

        List<TruckOptionsDto> result = truckOptionService.getPublicTruckOptions("LCV");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTruckType()).isEqualTo("LCV");
        assertThat(result.get(0).getBodyTypes()).contains("LCV_OPEN", "LCV_CONTAINER");
    }

    @Test
    @DisplayName("createTruckOption successfully creates a new option")
    void testCreateTruckOption() {
        TruckOptionsDto dto = TruckOptionsDto.builder()
                .truckType("HEAVY_TRAILER")
                .displayName("Heavy Duty Trailer")
                .active(true)
                .bodyTypes(List.of("LOWBED_TRAILER"))
                .availableLengthsFeet(List.of("50 Feet"))
                .availableWheelers(List.of("26 Wheeler"))
                .availablePassingTons(List.of("40-55 Ton"))
                .build();

        when(repository.existsByTruckTypeIgnoreCaseAndIsDeletedFalse("HEAVY_TRAILER")).thenReturn(false);
        when(repository.save(any(TruckOption.class))).thenAnswer(inv -> {
            TruckOption toSave = inv.getArgument(0);
            toSave.setId(10L);
            return toSave;
        });

        TruckOptionsDto created = truckOptionService.createTruckOption(dto);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(10L);
        assertThat(created.getTruckType()).isEqualTo("HEAVY_TRAILER");
        verify(repository).save(any(TruckOption.class));
    }

    @Test
    @DisplayName("updateTruckOption updates existing option")
    void testUpdateTruckOption() {
        when(repository.findById(1L)).thenReturn(Optional.of(lcvOption));
        when(repository.save(any(TruckOption.class))).thenAnswer(inv -> inv.getArgument(0));

        TruckOptionsDto updateDto = TruckOptionsDto.builder()
                .displayName("LCV Updated")
                .bodyTypes(List.of("LCV_OPEN", "LCV_CONTAINER", "LCV_REEFER"))
                .build();

        TruckOptionsDto updated = truckOptionService.updateTruckOption(1L, updateDto);

        assertThat(updated.getDisplayName()).isEqualTo("LCV Updated");
        assertThat(updated.getBodyTypes()).contains("LCV_REEFER");
    }
}
