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
@Table(name = "listing_location")
public class ListingLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long listingId;

    private String locationType;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private Double lat;
    private Double lng;
    private Integer sequence;
    private LocalDateTime expectedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
