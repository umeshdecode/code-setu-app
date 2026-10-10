package com.cargosetu.marketplace.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "marketplace_trip_event")
public class TripEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long tripId;

    private String eventType;

    private LocalDateTime eventTime;

    private String location;

    private Double lat;

    private Double lng;

    private Long actorStaffId;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    private String evidenceFileRef;

    private String previousStatus;

    private String newStatus;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
