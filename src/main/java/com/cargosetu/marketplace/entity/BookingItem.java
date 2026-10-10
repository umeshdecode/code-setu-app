package com.cargosetu.marketplace.entity;

import com.cargosetu.marketplace.enums.CapacityUnit;
import com.cargosetu.marketplace.enums.CargoType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "booking_item")
public class BookingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long bookingId;

    private Double loadWeight;

    private Double allocatedCapacity;

    @Enumerated(EnumType.STRING)
    private CapacityUnit capacityUnit;

    private Integer packageCount;

    @Enumerated(EnumType.STRING)
    private CargoType cargoType;

    private String cargoDescription;

    private BigDecimal declaredValue;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
