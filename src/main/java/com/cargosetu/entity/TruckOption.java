package com.cargosetu.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "truck_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TruckOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "truck_type", nullable = false, unique = true, length = 50)
    private String truckType; // e.g. LCV, OPEN_TRUCK, CONTAINER, TIPPER, TANKER, TRAILER, REEFER

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "truck_option_body_types", joinColumns = @JoinColumn(name = "truck_option_id"))
    @Column(name = "body_type")
    @Builder.Default
    private List<String> bodyTypes = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "truck_option_lengths", joinColumns = @JoinColumn(name = "truck_option_id"))
    @Column(name = "length_feet_option")
    @Builder.Default
    private List<String> availableLengthsFeet = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "truck_option_wheelers", joinColumns = @JoinColumn(name = "truck_option_id"))
    @Column(name = "wheeler_option")
    @Builder.Default
    private List<String> availableWheelers = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "truck_option_passing_tons", joinColumns = @JoinColumn(name = "truck_option_id"))
    @Column(name = "passing_ton_option")
    @Builder.Default
    private List<String> availablePassingTons = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
