package com.cargosetu.marketplace.entity;

import com.cargosetu.marketplace.enums.CargoType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "marketplace_saved_search")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedSearch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer companyId;
    private Long staffId;
    private String searchType;
    private String originCity;
    private String destinationCity;
    private String truckType;
    private Double truckLength;

    @Enumerated(EnumType.STRING)
    private CargoType cargoType;

    private Double minCapacity;
    private BigDecimal maxPrice;

    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
