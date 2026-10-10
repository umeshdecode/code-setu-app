package com.cargosetu.marketplace.entity;

import com.cargosetu.marketplace.enums.VerificationStatus;
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
@Table(name = "listing_document")
public class ListingDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long listingId;

    private String documentType;
    private String fileRef;
    private Long uploadedByStaffId;

    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
